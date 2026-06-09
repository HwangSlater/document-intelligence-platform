package com.documentintelligenceapplication.presentation.exception;

public abstract class DocumentIntelligenceException extends RuntimeException {
    
    private final String code;
    private final int status;

    protected DocumentIntelligenceException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
