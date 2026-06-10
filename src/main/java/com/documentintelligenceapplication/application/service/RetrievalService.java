package com.documentintelligenceapplication.application.service;

import com.documentintelligenceapplication.domain.dto.SearchResultResponse;
import com.documentintelligenceapplication.infrastructure.storage.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore embeddingStore;

    /**
     * 전체 문서를 대상으로 유사도 기반 청크 검색을 수행합니다.
     * 1. 사용자 질의(Query) 텍스트 검증
     * 2. OpenAI Embedding API를 통한 질의 벡터 생성
     * 3. DB(EmbeddingStore) 유사도 쿼리 실행 및 결과 DTO 반환
     */
    public List<SearchResultResponse> search(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("Search query is empty or null.");
            return Collections.emptyList();
        }

        if (limit <= 0) {
            limit = 5; // default fallback
        }

        log.info("Generating query embedding for query: '{}'", query);
        float[] queryVector;
        try {
            queryVector = embeddingModel.embed(query);
        } catch (Exception e) {
            log.error("Failed to generate embedding for search query: '{}'", query, e);
            throw new RuntimeException("Failed to generate embedding for query", e);
        }

        log.info("Performing similarity search with limit: {}", limit);
        return embeddingStore.findSimilarChunks(queryVector, limit);
    }
}
