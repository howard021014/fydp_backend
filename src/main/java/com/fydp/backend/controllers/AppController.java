package com.fydp.backend.controllers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    private static final String UPLOAD_PATH = System.getProperty("user.dir") + "/upload_files/";

    @RequestMapping("/")
    public String welcome(Model model) {
        logger.debug("Welcome endpoint hit");
        model.addAttribute("user", "demo_user");
        return "index";
    }

    @PostMapping(value =("/upload"),headers=("content-type=multipart/*"))
    public String upload(@RequestParam("file") MultipartFile file, Model model) throws IOException {
        logger.debug("Upload endpoint hit");

        String pdfText = "";
        List<String> bookmarks = new ArrayList<>();
        PDDocument document = parsePDF(loadPdfFile(file));
        if (document != null) {
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            if (outline != null) {
                storeBookmarks(outline, bookmarks);
            } else {
                pdfText =  new PDFTextStripper().getText(document);
            }
        } else {
            logger.error("Not able to load PDF");
        }

        document.close();
        model.addAttribute("pdf_text", pdfText.isEmpty() ? bookmarks : pdfText);
        return "output";
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

    private void storeBookmarks(PDOutlineNode bookmark, List<String> bookmarks) {
        PDOutlineItem current = bookmark.getFirstChild();
        while (current != null)
        {
            bookmarks.add(current.getTitle());
            storeBookmarks(current, bookmarks);
            current = current.getNextSibling();
        }
    }
}
