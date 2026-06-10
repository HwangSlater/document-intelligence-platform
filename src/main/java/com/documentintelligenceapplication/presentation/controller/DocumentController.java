package com.documentintelligenceapplication.presentation.controller;

import com.documentintelligenceapplication.application.service.DocumentService;
import com.documentintelligenceapplication.application.service.EmbeddingService;
import com.documentintelligenceapplication.presentation.dto.DocumentListResponse;
import com.documentintelligenceapplication.presentation.dto.DocumentUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final EmbeddingService embeddingService;

    @PostMapping
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        DocumentUploadResponse response = documentService.processUpload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentListResponse>> getDocuments() {
        List<DocumentListResponse> response = documentService.getDocuments();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable("id") UUID id) {
        documentService.deleteDocument(id);
    }

    @PostMapping("/{id}/embed")
    @ResponseStatus(HttpStatus.OK)
    public void embedDocument(@PathVariable("id") UUID id) {
        embeddingService.embedDocument(id);
    }
}

