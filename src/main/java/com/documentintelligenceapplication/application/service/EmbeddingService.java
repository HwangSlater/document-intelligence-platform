package com.documentintelligenceapplication.application.service;

import com.documentintelligenceapplication.domain.entity.Chunk;
import com.documentintelligenceapplication.domain.entity.Document;
import com.documentintelligenceapplication.domain.repository.ChunkRepository;
import com.documentintelligenceapplication.domain.repository.DocumentRepository;
import com.documentintelligenceapplication.infrastructure.storage.EmbeddingStore;
import com.documentintelligenceapplication.presentation.exception.DocumentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final EmbeddingStore embeddingStore;
    private final EmbeddingModel embeddingModel;

    /**
     * 문서를 임베딩하여 PGVector 데이터베이스에 저장합니다.
     * 1. 지정된 문서와 하위 청크 리스트 조회
     * 2. 동일 문서의 기존 임베딩 삭제 (멱등성 확보)
     * 3. OpenAI Embedding API Batch 호출
     * 4. JdbcTemplate을 통한 일괄/개별 임베딩 적재 및 통계 로깅
     */
    @Transactional
    public void embedDocument(UUID documentId) {
        // 1. 문서 및 청크 리스트 조회
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));

        List<Chunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        if (chunks.isEmpty()) {
            log.warn("No chunks found for document ID: {}. Embedding process skipped.", documentId);
            log.info("[Embedding Stats] Chunk Count: 0, Batch Size: 0, Time: 0ms, Success: 0, Failure: 0");
            return;
        }

        // 2. 멱등성 보장: 기존 임베딩 삭제 (트랜잭션 내에서 실행)
        deleteExistingEmbeddingsInTransaction(documentId);

        // 3. OpenAI Batch Embedding API 호출
        List<String> texts = chunks.stream().map(Chunk::getContent).toList();
        
        log.info("Requesting batch embedding from OpenAI for {} chunks...", texts.size());
        long startTime = System.currentTimeMillis();
        EmbeddingResponse response;
        try {
            response = embeddingModel.embedForResponse(texts);
        } catch (Exception e) {
            log.error("Failed to generate embeddings from OpenAI API for document ID: {}", documentId, e);
            log.info("[Embedding Stats] Chunk Count: {}, Batch Size: {}, Time: 0ms, Success: 0, Failure: {}", 
                    chunks.size(), texts.size(), chunks.size());
            throw new RuntimeException("OpenAI Embedding API call failed", e);
        }
        long endTime = System.currentTimeMillis();
        long generationTimeMs = endTime - startTime;

        String modelName = response.getMetadata().getModel();
        if (modelName == null || modelName.isBlank()) {
            modelName = "openai-embedding-model";
        }
        List<Embedding> embeddings = response.getResults();

        // 4. JdbcTemplate을 사용해 DB 적재 및 결과 통계 수집
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            float[] vector = embeddings.get(i).getOutput();
            try {
                saveEmbeddingInTransaction(chunk.getId(), vector, modelName);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to save embedding to database for chunk ID: {}", chunk.getId(), e);
                failureCount++;
            }
        }

        // 통계 로깅
        log.info("[Embedding Stats] Document ID: {}, File Name: {}", document.getId(), document.getFileName());
        log.info("[Embedding Stats] Chunk 개수: {} 개", chunks.size());
        log.info("[Embedding Stats] Batch 크기: {} 개", texts.size());
        log.info("[Embedding Stats] Embedding 생성 시간: {} ms", generationTimeMs);
        log.info("[Embedding Stats] 저장 건수: {} 건", successCount);
        log.info("[Embedding Stats] 실패 건수: {} 건", failureCount);
    }

    @Transactional
    public void deleteExistingEmbeddingsInTransaction(UUID documentId) {
        embeddingStore.deleteByDocumentId(documentId);
    }

    @Transactional
    public void saveEmbeddingInTransaction(UUID chunkId, float[] embedding, String modelName) {
        embeddingStore.save(chunkId, embedding, modelName);
    }
}
