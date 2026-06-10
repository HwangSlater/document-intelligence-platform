package com.documentintelligenceapplication.infrastructure.storage;

import com.documentintelligenceapplication.domain.dto.SearchResultResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingStoreSimilarityTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private EmbeddingStore embeddingStore;

    @Test
    @DisplayName("findSimilarChunks는 코사인 거리가 가까운 순으로 LIMIT 범위 내에서 청크 리스트를 정확히 쿼리한다")
    @SuppressWarnings("unchecked")
    void findSimilarChunks_Success() throws SQLException {
        // Given
        float[] queryVector = new float[]{0.1f, -0.2f, 0.3f};
        int limit = 5;
        String vectorString = java.util.Arrays.toString(queryVector);

        UUID chunkId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String content = "Matching document segment";
        double similarity = 0.85;

        SearchResultResponse response = SearchResultResponse.builder()
                .chunkId(chunkId)
                .documentId(documentId)
                .content(content)
                .similarity(similarity)
                .build();

        // JdbcTemplate.query(...) 모킹
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(vectorString), eq(vectorString), eq(limit)))
                .thenReturn(List.of(response));

        // When
        List<SearchResultResponse> results = embeddingStore.findSimilarChunks(queryVector, limit);

        // Then
        assertThat(results).hasSize(1);
        SearchResultResponse result = results.get(0);
        assertThat(result.getChunkId()).isEqualTo(chunkId);
        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getSimilarity()).isEqualTo(similarity);

        verify(jdbcTemplate, times(1)).query(
                eq("SELECT     c.id AS chunk_id,     c.document_id AS document_id,     c.content AS content,     (1 - (e.embedding <=> ?::vector)) AS similarity FROM embeddings e INNER JOIN chunks c ON e.chunk_id = c.id ORDER BY e.embedding <=> ?::vector ASC LIMIT ?"),
                any(RowMapper.class),
                eq(vectorString),
                eq(vectorString),
                eq(limit)
        );
    }

    @Test
    @DisplayName("RowMapper는 ResultSet 행 데이터를 SearchResultResponse DTO 객체로 정상 매핑한다")
    @SuppressWarnings("unchecked")
    void rowMapper_MappingSuccess() throws SQLException {
        // Given
        float[] queryVector = new float[]{0.1f, -0.2f};
        int limit = 2;

        UUID expectedChunkId = UUID.randomUUID();
        UUID expectedDocId = UUID.randomUUID();
        String expectedContent = "Mapped content text";
        double expectedSimilarity = 0.92;

        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("chunk_id", UUID.class)).thenReturn(expectedChunkId);
        when(rs.getObject("document_id", UUID.class)).thenReturn(expectedDocId);
        when(rs.getString("content")).thenReturn(expectedContent);
        when(rs.getDouble("similarity")).thenReturn(expectedSimilarity);

        // query의 두 번째 인자인 RowMapper를 탈취하기 위해 ArgumentCaptor 사용
        doAnswer(invocation -> {
            RowMapper<SearchResultResponse> mapper = invocation.getArgument(1);
            SearchResultResponse mappedObj = mapper.mapRow(rs, 1);
            return List.of(mappedObj);
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), anyString(), anyString(), eq(limit));

        // When
        List<SearchResultResponse> results = embeddingStore.findSimilarChunks(queryVector, limit);

        // Then
        assertThat(results).hasSize(1);
        SearchResultResponse result = results.get(0);
        assertThat(result.getChunkId()).isEqualTo(expectedChunkId);
        assertThat(result.getDocumentId()).isEqualTo(expectedDocId);
        assertThat(result.getContent()).isEqualTo(expectedContent);
        assertThat(result.getSimilarity()).isEqualTo(expectedSimilarity);
    }
}
