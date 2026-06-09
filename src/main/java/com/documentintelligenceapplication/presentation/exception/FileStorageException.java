package com.documentintelligenceapplication.presentation.exception;

public class FileStorageException extends DocumentIntelligenceException {
    
    public FileStorageException(String message) {
        super("FILE_STORAGE_ERROR", 500, message);
    }
}
