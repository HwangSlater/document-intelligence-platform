package com.documentintelligenceapplication.presentation.controller;

import com.documentintelligenceapplication.application.service.RagService;
import com.documentintelligenceapplication.domain.dto.RagRequest;
import com.documentintelligenceapplication.domain.dto.RagResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag/ask")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * 사용자의 자연어 질문을 접수하여 RAG QA 답변을 생성하고 질문과 답변을 함께 반환합니다.
     * POST /api/v1/rag/ask
     */
    @PostMapping
    public ResponseEntity<RagResponse> askQuestion(@Valid @RequestBody RagRequest request) {
        RagResponse response = ragService.askQuestion(request);
        return ResponseEntity.ok(response);
    }
}
