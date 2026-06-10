package com.documentintelligenceapplication.domain.dto;

import lombok.Getter;

@Getter
public class RagResponse {
    private final String question;
    private final String answer;

    public RagResponse(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }
}
