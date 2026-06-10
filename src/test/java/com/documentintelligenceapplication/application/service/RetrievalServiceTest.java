package com.documentintelligenceapplication.application.service;

import com.documentintelligenceapplication.domain.dto.SearchResultResponse;
import com.documentintelligenceapplication.infrastructure.storage.EmbeddingStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore embeddingStore;

    @InjectMocks
    private RetrievalService retrievalService;

    @Test
    @DisplayName("검색 질의가 null이거나 비어있을 경우 빈 결과를 즉시 반환한다")
    void search_EmptyQuery() {
        // When
        List<SearchResultResponse> results1 = retrievalService.search(null, 5);
        List<SearchResultResponse> results2 = retrievalService.search("   ", 5);

        // Then
        assertThat(results1).isEmpty();
        assertThat(results2).isEmpty();
        verifyNoInteractions(embeddingModel, embeddingStore);
    }

    @Test
    @DisplayName("OpenAI 임베딩 API 호출이 실패할 경우 런타임 예외를 발생시킨다")
    void search_EmbeddingModelException() {
        // Given
        String query = "RAG retrieval flow";
        when(embeddingModel.embed(query)).thenThrow(new RuntimeException("API Connection timeout"));

        // When & Then
        assertThatThrownBy(() -> retrievalService.search(query, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate embedding for query");

        verifyNoInteractions(embeddingStore);
    }

    @Test
    @DisplayName("정상적인 질의를 받아 쿼리 벡터를 추출하고 EmbeddingStore에 검색을 올바르게 위임한다")
    void search_Success() {
        // Given
        String query = "Spring Boot 3.5";
        int limit = 3;
        float[] expectedQueryVector = new float[]{0.15f, -0.25f, 0.35f};

        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        SearchResultResponse expectedMatch = SearchResultResponse.builder()
                .chunkId(chunkId)
                .documentId(docId)
                .content("Spring Boot 3.5 integration details")
                .similarity(0.89)
                .build();

        when(embeddingModel.embed(query)).thenReturn(expectedQueryVector);
        when(embeddingStore.findSimilarChunks(expectedQueryVector, limit)).thenReturn(List.of(expectedMatch));

        // When
        List<SearchResultResponse> results = retrievalService.search(query, limit);

        // Then
        assertThat(results).hasSize(1);
        SearchResultResponse match = results.get(0);
        assertThat(match.getChunkId()).isEqualTo(chunkId);
        assertThat(match.getDocumentId()).isEqualTo(docId);
        assertThat(match.getContent()).isEqualTo("Spring Boot 3.5 integration details");
        assertThat(match.getSimilarity()).isEqualTo(0.89);

        verify(embeddingModel, times(1)).embed(query);
        verify(embeddingStore, times(1)).findSimilarChunks(expectedQueryVector, limit);
    }
}
