package com.documentintelligenceapplication.infrastructure.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingStoreTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private EmbeddingStore embeddingStore;

    @Test
    @DisplayName("initSchema는 vector extension 활성화 및 embeddings 테이블, 복합 인덱스 DDL을 차례대로 수행한다")
    void initSchema_Success() {
        // When
        embeddingStore.initSchema();

        // Then
        verify(jdbcTemplate, times(1)).execute("CREATE EXTENSION IF NOT EXISTS vector");
        verify(jdbcTemplate, times(1)).execute(contains("CREATE TABLE IF NOT EXISTS embeddings"));
        verify(jdbcTemplate, times(1)).execute("CREATE INDEX IF NOT EXISTS idx_embeddings_chunk_model ON embeddings (chunk_id, model_name)");
    }

    @Test
    @DisplayName("initSchema 수행 시 예외가 발생하더라도 예외를 던지지 않고 경고 로그 처리 후 빌드를 진행한다")
    void initSchema_ExceptionSwallowed() {
        // Given
        doThrow(new RuntimeException("Connection refused")).when(jdbcTemplate).execute(anyString());

        // When & Then (예외가 발생하지 않고 조용히 끝나는지 검증)
        embeddingStore.initSchema();

        verify(jdbcTemplate, times(1)).execute("CREATE EXTENSION IF NOT EXISTS vector");
    }

    @Test
    @DisplayName("save는 chunkId, vector string, modelName을 바인딩하여 INSERT SQL을 실행한다")
    void save_Success() {
        // Given
        UUID chunkId = UUID.randomUUID();
        float[] embedding = new float[]{0.1f, -0.2f, 0.3f};
        String modelName = "test-model";

        // When
        embeddingStore.save(chunkId, embedding, modelName);

        // Then
        verify(jdbcTemplate, times(1)).update(
                eq("INSERT INTO embeddings (id, chunk_id, embedding, model_name, created_at) VALUES (?, ?, ?::vector, ?, ?)"),
                any(UUID.class),
                eq(chunkId),
                eq("[0.1, -0.2, 0.3]"),
                eq(modelName),
                any()
        );
    }

    @Test
    @DisplayName("deleteByDocumentId는 documentId와 chunks inner 조인을 사용하여 기존 embeddings를 일괄 삭제한다")
    void deleteByDocumentId_Success() {
        // Given
        UUID documentId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(documentId))).thenReturn(5);

        // When
        embeddingStore.deleteByDocumentId(documentId);

        // Then
        verify(jdbcTemplate, times(1)).update(
                eq("DELETE FROM embeddings WHERE chunk_id IN (SELECT id FROM chunks WHERE document_id = ?)"),
                eq(documentId)
        );
    }
}
