package com.fydp.backend.controllers;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
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

    @RequestMapping("/")
    public String welcome() {
        logger.debug("Welcome endpoint hit");
        return "This is the homepage.";
    }

    @PostMapping("/upload")
    public void upload(@RequestParam("file") MultipartFile file) {
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

        parsePDF(pdfFile);
    }

    private void parsePDF(File file) {
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext pcontext = new ParseContext();
        PDFParser pdfparser = new PDFParser();
        try (FileInputStream inputstream = new FileInputStream(file)) {
            // parsing the document using PDF parser
            pdfparser.parse(inputstream, handler, metadata,pcontext);

            // output to logger for now
            logger.info("Document content: " + handler.toString());
            logger.info("Document metadata: ");
            String[] names = metadata.names();
            for (String name : names) {
                logger.info(name + ":  " + metadata.get(name));
            }

        } catch (Exception ex) {
            logger.error("Error occurred while parsing file", ex);
        }

    }
}
