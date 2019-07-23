package com.fydp.backend.model;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChapterTextModel {
    private Map<String, String> chpTxt;

    public Map<String, String> getChpTextMap() {
        return chpTxt;
    }

    public void setChpTextMap(Map<String, String> chpTextMap) {
        this.chpTxt = chpTextMap;
    }
}
