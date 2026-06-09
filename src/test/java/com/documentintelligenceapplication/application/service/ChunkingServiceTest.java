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
    void runChunkingOptimizationExperiment() {
        // Given: 15,000자 분량의 더 길고 정교한 문서 텍스트 시뮬레이션
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 80; i++) {
            sb.append("Sentence ").append(i)
              .append(": The Document Intelligence Platform leverages Spring Boot 3.5.x and Spring AI to process high-volume documents. ")
              .append("By implementing modern Retrieval-Augmented Generation (RAG) pipelines, the system splits texts into logical segments. ")
              .append("This is an important optimization phase where we evaluate the context density, chunk size, and chunk overlap. ")
              .append("We aim to find the best configuration that minimizes context fragmentation while fitting within the model limits.\n\n");
        }
        String text = sb.toString();

        Document document = Document.builder()
                .fileName("optimization-test.pdf")
                .filePath("/mock/path")
                .fileType("pdf")
                .fileSize((long) text.length())
                .uploadDate(LocalDateTime.now())
                .processingStatus(ProcessingStatus.UPLOADED)
                .build();

        int[] sizes = {300, 500, 800, 1000};
        
        System.out.println("==================================================");
        System.out.println("       CHUNKING OPTIMIZATION EXPERIMENT           ");
        System.out.println("       Total Document Chars: " + text.length());
        System.out.println("==================================================");

        for (int size : sizes) {
            // Overlap은 size의 20%로 동적 할당
            int overlap = size / 5;
            ReflectionTestUtils.setField(chunkingService, "chunkSize", size);
            ReflectionTestUtils.setField(chunkingService, "chunkOverlap", overlap);

            System.out.println("\n[Experiment Configuration] Chunk Size: " + size + " Tokens, Overlap: " + overlap + " Chars");
            chunkingService.splitAndSave(document, text);
        }
        System.out.println("==================================================");
    }
}
