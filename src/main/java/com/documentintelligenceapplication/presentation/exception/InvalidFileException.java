package com.documentintelligenceapplication.presentation.exception;

public class InvalidFileException extends DocumentIntelligenceException {
    
    public InvalidFileException(String message) {
        super("INVALID_FILE_FORMAT", 400, message);
    }
}
