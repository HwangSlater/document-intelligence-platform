package com.documentintelligenceapplication.presentation.exception;

public class DocumentNotFoundException extends DocumentIntelligenceException {
    
    public DocumentNotFoundException(String message) {
        super("DOCUMENT_NOT_FOUND", 404, message);
    }
}
