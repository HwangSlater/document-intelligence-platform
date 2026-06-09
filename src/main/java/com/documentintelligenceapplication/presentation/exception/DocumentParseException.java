package com.documentintelligenceapplication.presentation.exception;

public class DocumentParseException extends DocumentIntelligenceException {
    
    public DocumentParseException(String message) {
        super("PARSING_FAILED", 500, message);
    }
}
