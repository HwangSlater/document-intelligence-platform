package com.documentintelligenceapplication.presentation.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String code,
    String message
) {}
