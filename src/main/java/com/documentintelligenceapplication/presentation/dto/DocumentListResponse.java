package com.documentintelligenceapplication.presentation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentListResponse(
    UUID id,
    String fileName,
    long fileSize,
    String status,
    LocalDateTime uploadDate
) {}
