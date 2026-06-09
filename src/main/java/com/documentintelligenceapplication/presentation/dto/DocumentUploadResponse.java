package com.documentintelligenceapplication.presentation.dto;

import java.util.UUID;

public record DocumentUploadResponse(
    UUID documentId,
    String fileName,
    String status
) {}
