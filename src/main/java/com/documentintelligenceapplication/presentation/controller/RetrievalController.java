package com.documentintelligenceapplication.presentation.controller;

import com.documentintelligenceapplication.application.service.RetrievalService;
import com.documentintelligenceapplication.domain.dto.SearchResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class RetrievalController {

    private final RetrievalService retrievalService;

    /**
     * 전체 문서에서 질의와 유사한 Chunk 목록을 조회하여 코사인 유사도 점수와 함께 반환합니다.
     * GET /api/v1/search?query=검색어&limit=5
     */
    @GetMapping
    public ResponseEntity<List<SearchResultResponse>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        List<SearchResultResponse> results = retrievalService.search(query, limit);
        return ResponseEntity.ok(results);
    }
}