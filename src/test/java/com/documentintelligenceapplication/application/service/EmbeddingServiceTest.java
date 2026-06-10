package com.documentintelligenceapplication.application.service;

import com.documentintelligenceapplication.domain.entity.Chunk;
import com.documentintelligenceapplication.domain.entity.Document;
import com.documentintelligenceapplication.domain.entity.ProcessingStatus;
import com.documentintelligenceapplication.domain.repository.ChunkRepository;
import com.documentintelligenceapplication.domain.repository.DocumentRepository;
import com.documentintelligenceapplication.infrastructure.storage.EmbeddingStore;
import com.documentintelligenceapplication.presentation.exception.DocumentNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private EmbeddingStore embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("존재하지 않는 문서 ID로 임베딩을 요청하면 DocumentNotFoundException이 발생한다")
    void embedDocument_NotFound() {
        // Given
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> embeddingService.embedDocument(documentId))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document not found with ID:");

        verifyNoInteractions(chunkRepository, embeddingStore, embeddingModel);
    }

    @Test
    @DisplayName("청크가 없는 문서의 경우 임베딩 처리를 건너뛴다")
    void embedDocument_NoChunks() {
        // Given
        UUID documentId = UUID.randomUUID();
        Document document = Document.builder()
                .fileName("test.pdf")
                .filePath("/mock/path")
                .fileType("pdf")
                .fileSize(100L)
                .uploadDate(LocalDateTime.now())
                .processingStatus(ProcessingStatus.UPLOADED)
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId)).thenReturn(Collections.emptyList());

        // When
        embeddingService.embedDocument(documentId);

        // Then
        verify(embeddingStore, never()).deleteByDocumentId(any());
        verifyNoInteractions(embeddingModel);
    }

    @Test
    @DisplayName("정상적인 청크 목록을 받아 Batch 임베딩을 생성하고 DB에 저장한다")
    void embedDocument_Success() {
        // Given
        UUID documentId = UUID.randomUUID();
        Document document = Document.builder()
                .fileName("test.pdf")
                .filePath("/mock/path")
                .fileType("pdf")
                .fileSize(100L)
                .uploadDate(LocalDateTime.now())
                .processingStatus(ProcessingStatus.UPLOADED)
                .build();

        Chunk chunk1 = Chunk.builder()
                .document(document)
                .chunkIndex(0)
                .content("First chunk content")
                .createdAt(LocalDateTime.now())
                .build();
        // ID 강제 설정을 모킹하기 위해 리플렉션 대신, 빌더 등으로 처리 불가능하므로 reflection 사용 또는 Mock 사용 가능.
        // Chunk 객체에 UUID 필드가 있으므로 Mockito로 id 리턴하거나 Mockito.spy 활용 또는 직접 setField 가능.
        org.springframework.test.util.ReflectionTestUtils.setField(chunk1, "id", UUID.randomUUID());

        Chunk chunk2 = Chunk.builder()
                .document(document)
                .chunkIndex(1)
                .content("Second chunk content")
                .createdAt(LocalDateTime.now())
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(chunk2, "id", UUID.randomUUID());

        List<Chunk> chunks = List.of(chunk1, chunk2);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId)).thenReturn(chunks);

        // Mock OpenAI API Response
        float[] vector1 = new float[]{0.1f, 0.2f, 0.3f};
        float[] vector2 = new float[]{0.4f, 0.5f, 0.6f};
        Embedding embeddingResult1 = new Embedding(vector1, 0);
        Embedding embeddingResult2 = new Embedding(vector2, 1);

        EmbeddingResponseMetadata responseMetadata = new EmbeddingResponseMetadata("text-embedding-ada-002", null);
        EmbeddingResponse embeddingResponse = new EmbeddingResponse(List.of(embeddingResult1, embeddingResult2), responseMetadata);

        when(embeddingModel.embedForResponse(List.of("First chunk content", "Second chunk content")))
                .thenReturn(embeddingResponse);

        // When
        embeddingService.embedDocument(documentId);

        // Then
        // 멱등 삭제 확인
        verify(embeddingStore, times(1)).deleteByDocumentId(documentId);
        // 저장 확인
        verify(embeddingStore, times(1)).save(eq(chunk1.getId()), eq(vector1), eq("text-embedding-ada-002"));
        verify(embeddingStore, times(1)).save(eq(chunk2.getId()), eq(vector2), eq("text-embedding-ada-002"));
    }
}
