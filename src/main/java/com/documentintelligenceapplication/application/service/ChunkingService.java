package com.documentintelligenceapplication.application.service;

import com.documentintelligenceapplication.domain.entity.Chunk;
import com.documentintelligenceapplication.domain.entity.Document;
import com.documentintelligenceapplication.domain.repository.ChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final ChunkRepository chunkRepository;

    @Value("${app.chunking.size}")
    private int chunkSize;

    @Value("${app.chunking.overlap}")
    private int chunkOverlap;

    @Transactional
    public void splitAndSave(Document document, String text) {
        if (text == null || text.isBlank()) {
            log.warn("Extracted text is empty. No chunks created for document: {}", document.getId());
            return;
        }

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(chunkOverlap)
                .build();

        org.springframework.ai.document.Document originalDoc = 
                new org.springframework.ai.document.Document(text);

        List<org.springframework.ai.document.Document> splitDocs = splitter.split(List.of(originalDoc));

        int documentLength = text.length();
        int chunkCount = splitDocs.size();
        double averageChunkSize = splitDocs.stream()
                .mapToInt(doc -> doc.getText().length())
                .average()
                .orElse(0.0);
        int maxChunkSize = splitDocs.stream()
                .mapToInt(doc -> doc.getText().length())
                .max()
                .orElse(0);

        log.info("[Chunking Stats] Document ID: {}, File Name: {}", document.getId(), document.getFileName());
        log.info("[Chunking Stats] 문서 길이: {} 자", documentLength);
        log.info("[Chunking Stats] Chunk 개수: {} 개", chunkCount);
        log.info("[Chunking Stats] 평균 Chunk 크기: {} 자", String.format("%.2f", averageChunkSize));
        log.info("[Chunking Stats] 최대 Chunk 크기: {} 자", maxChunkSize);

        for (int i = 0; i < splitDocs.size(); i++) {
            org.springframework.ai.document.Document splitDoc = splitDocs.get(i);
            Chunk chunk = Chunk.builder()
                    .document(document)
                    .chunkIndex(i)
                    .content(splitDoc.getText())
                    .createdAt(LocalDateTime.now())
                    .build();
            chunkRepository.save(chunk);
        }
    }
}
