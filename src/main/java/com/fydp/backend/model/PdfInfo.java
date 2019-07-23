package com.fydp.backend.model;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PdfInfo {
    private Map<String, Integer> chapterPgMap;
    private Set<String> chapters;
    private String pdfText;

    public PdfInfo() {

    }

    public void setChapterPgMap(Map<String, Integer> chapterPgMap) {
        this.chapterPgMap = chapterPgMap;
    }

    public Map<String, Integer> getChapterPgMap() {
        return chapterPgMap;
    }

    public Set<String> getChapters() {
        return chapterPgMap.keySet();
    }

    public void setPdfText(String pdfText) {
        this.pdfText = pdfText;
    }

    public String getPdfText() {
        return pdfText;
    }

}
