package com.documentintelligenceapplication.domain.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class SearchResultResponse {
    private final UUID chunkId;
    private final UUID documentId;
    private final String content;
    private final Double similarity;

    public SearchResultResponse(UUID chunkId, UUID documentId, String content, Double similarity) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.content = content;
        this.similarity = similarity;
    }
}
