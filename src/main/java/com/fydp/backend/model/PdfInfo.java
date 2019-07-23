package com.fydp.backend.model;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PdfInfo {
    private Map<String, Integer> chapterPgMap;
    private List<String> chapters;
    private String pdfText;
    private String fileName;

    public PdfInfo() {

    }

    public void setChapterPgMap(Map<String, Integer> chapterPgMap) {
        this.chapterPgMap = chapterPgMap;
    }

    public Map<String, Integer> getChapterPgMap() {
        return chapterPgMap;
    }

    public void setChapters(List<String> chapters) {
        this.chapters = chapters;
    }

    public List<String> getChapters() {
        return chapters;
    }

    public void setPdfText(String pdfText) {
        this.pdfText = pdfText;
    }

    public String getPdfText() {
        return pdfText;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
