package com.documentintelligenceapplication.application.service;

import com.documentintelligenceapplication.domain.entity.Document;
import com.documentintelligenceapplication.domain.entity.ProcessingStatus;
import com.documentintelligenceapplication.domain.repository.DocumentRepository;
import com.documentintelligenceapplication.infrastructure.parser.DocumentParser;
import com.documentintelligenceapplication.infrastructure.storage.FileStorageService;
import com.documentintelligenceapplication.presentation.dto.DocumentListResponse;
import com.documentintelligenceapplication.presentation.dto.DocumentUploadResponse;
import com.documentintelligenceapplication.presentation.exception.DocumentNotFoundException;
import com.documentintelligenceapplication.presentation.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentParser documentParser;
    private final FileStorageService fileStorageService;

    @Transactional
    public DocumentUploadResponse processUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or not provided.");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidFileException("Only PDF files are supported.");
        }

        File storedFile = fileStorageService.store(file);

        try {
            Document document = Document.builder()
                    .fileName(originalFilename)
                    .filePath(storedFile.getAbsolutePath())
                    .fileType("pdf")
                    .fileSize(file.getSize())
                    .uploadDate(LocalDateTime.now())
                    .processingStatus(ProcessingStatus.UPLOADED)
                    .build();

            String extractedText = documentParser.parse(storedFile);
            document.updateExtractedText(extractedText);

            Document savedDocument = documentRepository.save(document);

            return new DocumentUploadResponse(
                    savedDocument.getId(),
                    savedDocument.getFileName(),
                    savedDocument.getProcessingStatus().name()
            );
        } catch (Exception e) {
            fileStorageService.delete(storedFile.getAbsolutePath());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentListResponse> getDocuments() {
        return documentRepository.findAll().stream()
                .map(doc -> new DocumentListResponse(
                        doc.getId(),
                        doc.getFileName(),
                        doc.getFileSize(),
                        doc.getProcessingStatus().name(),
                        doc.getUploadDate()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + id));

        fileStorageService.delete(document.getFilePath());
        documentRepository.delete(document);
    }
}
