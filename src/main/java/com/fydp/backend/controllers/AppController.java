package com.fydp.backend.controllers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@RestController
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    private static final String UPLOAD_PATH = System.getProperty("user.dir") + "/upload_files/";
    private PDDocument doc = null;

    @RequestMapping("/")
    public String welcome() {
        logger.debug("Welcome endpoint hit");
        return "This is the homepage.";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {
        logger.debug("Upload endpoint hit");

        File pdfFile = new File(UPLOAD_PATH + file.getOriginalFilename());
        try {
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

        String pdfText = parsePDF(pdfFile);
        parseBookmarks();

        doc.close();
        return pdfText;
    }

    private String parsePDF(File file) {
        try {
            doc = PDDocument.load(file);
            return new PDFTextStripper().getText(doc);
        } catch (IOException ex) {
            logger.error("Error loading the pdf file", ex);
        }

        return null;
    }

    private void parseBookmarks() {
        PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
        if (outline != null) {
            printBookmarks(outline);
        }
    }

    private void printBookmarks(PDOutlineNode bookmark) {
        PDOutlineItem current = bookmark.getFirstChild();
        while (current != null)
        {
            logger.info(current.getTitle());
            printBookmarks(current);
            current = current.getNextSibling();
        }
    }
}
