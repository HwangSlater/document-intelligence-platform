package com.documentintelligenceapplication.application.service;

import com.documentintelligenceapplication.domain.entity.Document;
import com.documentintelligenceapplication.domain.entity.ProcessingStatus;
import com.documentintelligenceapplication.domain.repository.ChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChunkingServiceTest {

    @Mock
    private ChunkRepository chunkRepository;

    @InjectMocks
    private ChunkingService chunkingService;

    @Test
    void testChunkingWithStatistics() {
        // Given
        ReflectionTestUtils.setField(chunkingService, "chunkSize", 1000);
        ReflectionTestUtils.setField(chunkingService, "chunkOverlap", 200);

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            sb.append("Sentence ").append(i)
              .append(": Document Intelligence Platform is designed to help users solve complex tasks using advanced agentic workflows. ")
              .append("We split documents into meaningful chunks for LLM ingestion.\n\n");
        }
        String text = sb.toString();

        Document document = Document.builder()
                .fileName("sample-spec.pdf")
                .filePath("/mock/path")
                .fileType("pdf")
                .fileSize((long) text.length())
                .uploadDate(LocalDateTime.now())
                .processingStatus(ProcessingStatus.UPLOADED)
                .build();

        // When
        chunkingService.splitAndSave(document, text);

        // Then
        verify(chunkRepository, atLeastOnce()).save(any());
    }
}
