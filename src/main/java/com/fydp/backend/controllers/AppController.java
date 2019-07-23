package com.fydp.backend.controllers;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    private static final String UPLOAD_PATH = System.getProperty("user.dir") + "/upload_files/";
    private static final String CHAPTER_REGEX = "^(?i)\\bChapter\\b";
    private Map<String, Integer> chapterPgMap = new LinkedHashMap<>();

    @Autowired
    private PdfInfo pdfInfo;

    @RequestMapping("/")
    public String welcome() {
        logger.debug("Welcome endpoint hit");
        return "index";
    }

    @PostMapping(value =("/upload"),headers=("content-type=multipart/*"))
    public PdfInfo upload(@RequestParam("file") MultipartFile file) throws IOException {
        logger.debug("Upload endpoint hit");

        String pdfText = "";
        PDDocument document = parsePDF(loadPdfFile(file));
        if (document != null) {
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            if (outline != null) {
                storeBookmarks(outline);
            } else {
                pdfText =  new PDFTextStripper().getText(document);
            }
        } else {
            logger.error("Not able to load PDF");
        }

        pdfInfo.setChapterPgMap(chapterPgMap);
        pdfInfo.setPdfText(pdfText);
        document .close();
        return pdfInfo;
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

        try(FileOutputStream os = new FileOutputStream(pdfFile);) {
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

    private void storeBookmarks(PDOutlineNode bookmark) throws IOException {
        PDOutlineItem current = bookmark.getFirstChild();
        Pattern pattern = Pattern.compile(CHAPTER_REGEX);
        while (current != null)
        {
            Matcher match = pattern.matcher(current.getTitle());
            if (match.find()) {
                PDActionGoTo action = (PDActionGoTo) current.getAction();
                PDPageDestination destination = (PDPageDestination) action.getDestination();
                int pageNum = 0;
                if (destination != null) {
                    pageNum = destination.retrievePageNumber() + 1;
                }
                chapterPgMap.put(current.getTitle(), pageNum);
            }

            storeBookmarks(current);
            current = current.getNextSibling();
        }
    }
}
