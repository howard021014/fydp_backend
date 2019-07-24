package com.fydp.backend.controllers;

import com.fydp.backend.kafka.KafkaProducer;
import com.fydp.backend.kafka.MessageListener;
import com.fydp.backend.model.ChapterTextModel;
import com.fydp.backend.model.PdfInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Note: A lot of code are basically recreating the PDF document as well as variables that should only be
 * retrieved once.
 * TODO: Refactor the code such that we can store and get data from database once the database is implemented
 */

@RestController
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    private static final String UPLOAD_PATH = System.getProperty("user.dir") + "/upload_files/";
    private static final String END_OF_CHAPTER = "End of Last Chapter";
    private static final String CHAPTER_REGEX = "^(?i)\\bChapter\\b";

    @Autowired
    private PdfInfo pdfInfo;

    @Autowired
    private ChapterTextModel chapterTextModel;

    @Autowired
    private MessageListener listener;

    @Autowired
    private KafkaProducer producer;

    @RequestMapping("/")
    public String welcome() {
        logger.debug("Welcome endpoint hit");
        return "index";
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value = ("/summaries"))
    public Map<String, String> getSummaries() {
        logger.info("GET summary endpoint hit");

        try {
            listener.getLatch().await();
        } catch (InterruptedException e) {
            logger.error("Error while waiting for kafka response : " + e.getMessage());
            e.printStackTrace();
        }

        return listener.getMessages();
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = ("/upload"), headers = ("content-type=multipart/*"))
    public PdfInfo upload(@RequestParam("file") MultipartFile file) throws IOException {
        logger.debug("Upload endpoint hit");

        boolean containsBookMarks = false;
        String pdfText = "";
        PDDocument document = parsePDF(loadPdfFile(file));
        Map<String, Integer> map = new LinkedHashMap<>();
        Map<String, Integer> chapterPgMap = new LinkedHashMap<>();
        if (document != null) {
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            if (outline != null) {
                containsBookMarks = true;
                storeBookmarks(outline, map, 0);
            } else {
                pdfText = new PDFTextStripper().getText(document);
            }
        } else {
            logger.error("Not able to load PDF");
        }

        if (containsBookMarks) {
            Pattern pattern = Pattern.compile(CHAPTER_REGEX);
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                Matcher match = pattern.matcher(entry.getKey());
                if (match.find()) {
                    chapterPgMap.put(entry.getKey(), entry.getValue());
                }
            }

            List<String> allChapters = new ArrayList<>(map.keySet());
            List<String> chapters = new ArrayList<>(chapterPgMap.keySet());

            if (allChapters.size() == chapters.size()) {
                chapterPgMap.put(END_OF_CHAPTER, document.getNumberOfPages());
            } else {
                String lastChapter = chapters.get(chapters.size() - 1);
                String endOfLastChapter = allChapters.get(allChapters.indexOf(lastChapter) + 1);
                chapterPgMap.put(endOfLastChapter, map.get(endOfLastChapter));
            }

            pdfInfo.setChapters(chapters);
            pdfInfo.setChapterPgMap(chapterPgMap);
        }

        pdfInfo.setPdfText(pdfText);
        pdfInfo.setFileName(file.getOriginalFilename());
        if (!pdfText.isEmpty()) {
            producer.sendMessage(pdfText);
            listener.setMessages(1);
        }
        document.close();
        return pdfInfo;
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/upload/chapters", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ChapterTextModel parseChapters(@RequestBody PdfInfo response) throws IOException {
        List<String> chapters = response.getChapters();
        Map<String, Integer> pgMap = response.getChapterPgMap();
        List<String> refChapters = new ArrayList<>(pgMap.keySet());
        Map<String, String> chapterTxt = new HashMap<>();
        PDDocument document = parsePDF(new File(UPLOAD_PATH + response.getFileName()));
        for (String chapter : chapters) {
            int startPg = pgMap.get(chapter);
            int endPg = pgMap.get(refChapters.get(refChapters.indexOf(chapter) + 1));
            try {
                PDFTextStripper reader = new PDFTextStripper();
                reader.setStartPage(startPg);
                reader.setEndPage(endPg - 1);
                chapterTxt.put(chapter, reader.getText(document));
            } catch (IOException ex) {
                logger.error("Unable to create text stripper", ex);
            }
        }

        chapterTextModel.setChpTextMap(chapterTxt);
        for (Map.Entry entry : chapterTxt.entrySet()) {
            producer.sendMessageWithKey(entry.getValue().toString(), entry.getKey().toString());
        }
        listener.setMessages(chapterTxt.size());

        document.close();
        return chapterTextModel;
    }

    private File loadPdfFile(MultipartFile file) {
        File pdfFile = new File(UPLOAD_PATH + file.getOriginalFilename());
        try {
            if (!Files.exists(Paths.get(UPLOAD_PATH))) {
                Files.createDirectory(Paths.get(UPLOAD_PATH));
            }
            if (!pdfFile.exists()) {
                pdfFile.createNewFile();
            }
        } catch (IOException ex) {
            logger.error("Unable to create new File", ex);
        }

        try (FileOutputStream os = new FileOutputStream(pdfFile);) {
            os.write(file.getBytes());
        } catch (IOException ex) {
            logger.error("Error occurred while writing to file", ex);
        }

        return pdfFile;
    }

    private PDDocument parsePDF(File file) {
        PDDocument doc = null;
        try {
            doc = PDDocument.load(file);
            return doc;
        } catch (IOException ex) {
            logger.error("Error loading the pdf file", ex);
        }
        return doc;
    }

    private void storeBookmarks(PDOutlineNode bookmark, Map<String, Integer> map, int depth) throws IOException {
        PDOutlineItem current = bookmark.getFirstChild();

        while (current != null) {
            if (depth == 2) {
                break;
            }
            PDActionGoTo action = (PDActionGoTo) current.getAction();
            PDPageDestination destination = (PDPageDestination) action.getDestination();
            int pageNum = 0;
            if (destination != null) {
                pageNum = destination.retrievePageNumber() + 1;
            }
            map.put(current.getTitle(), pageNum);
            storeBookmarks(current, map, depth + 1);
            current = current.getNextSibling();
        }
    }

}
