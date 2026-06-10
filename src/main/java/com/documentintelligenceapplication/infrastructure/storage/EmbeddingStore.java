package com.documentintelligenceapplication.infrastructure.storage;

import com.documentintelligenceapplication.domain.dto.SearchResultResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EmbeddingStore {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initSchema() {
        log.info("Initializing pgvector and embeddings schema...");
        try {
            // 1. pgvector 확장 모듈 활성화
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

            // 2. embeddings 테이블 생성 (chunk_id UNIQUE 제약 조건 포함)
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS embeddings (" +
                    "    id UUID PRIMARY KEY," +
                    "    chunk_id UUID NOT NULL UNIQUE," +
                    "    embedding VECTOR(1536) NOT NULL," +
                    "    model_name VARCHAR(100) NOT NULL," +
                    "    created_at TIMESTAMP NOT NULL," +
                    "    CONSTRAINT fk_embeddings_chunk FOREIGN KEY (chunk_id) " +
                    "        REFERENCES chunks (id) ON DELETE CASCADE" +
                    ")"
            );

            // 3. 복합 인덱스 idx_embeddings_chunk_model 생성
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_embeddings_chunk_model ON embeddings (chunk_id, model_name)"
            );
            log.info("Successfully initialized embeddings schema.");
        } catch (Exception e) {
            // H2 등 pgvector를 미지원하는 테스트 DB 혹은 오프라인 환경 등에서 빌드 중단 방지
            log.warn("Failed to initialize embeddings schema. This is expected if you are running offline build, using H2 database or if PGVector extension is not installed: {}", e.getMessage());
        }
    }

    public void save(UUID chunkId, float[] embedding, String modelName) {
        String sql = "INSERT INTO embeddings (id, chunk_id, embedding, model_name, created_at) VALUES (?, ?, ?::vector, ?, ?)";
        String vectorString = java.util.Arrays.toString(embedding); // Format: "[0.123, -0.456, ...]"
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(sql, id, chunkId, vectorString, modelName, LocalDateTime.now());
    }

    public void deleteByDocumentId(UUID documentId) {
        String sql = "DELETE FROM embeddings WHERE chunk_id IN (SELECT id FROM chunks WHERE document_id = ?)";
        int deletedRows = jdbcTemplate.update(sql, documentId);
        log.info("Deleted {} existing embeddings for document ID: {}", deletedRows, documentId);
    }

    public List<SearchResultResponse> findSimilarChunks(float[] queryVector, int limit) {
        String sql = "SELECT " +
                     "    c.id AS chunk_id, " +
                     "    c.document_id AS document_id, " +
                     "    c.content AS content, " +
                     "    (1 - (e.embedding <=> ?::vector)) AS similarity " +
                     "FROM embeddings e " +
                     "INNER JOIN chunks c ON e.chunk_id = c.id " +
                     "ORDER BY e.embedding <=> ?::vector ASC " +
                     "LIMIT ?";
        String vectorString = java.util.Arrays.toString(queryVector);
        return jdbcTemplate.query(sql, (rs, rowNum) -> 
            SearchResultResponse.builder()
                .chunkId(rs.getObject("chunk_id", UUID.class))
                .documentId(rs.getObject("document_id", UUID.class))
                .content(rs.getString("content"))
                .similarity(rs.getDouble("similarity"))
                .build(),
            vectorString, vectorString, limit
        );
    }
}

