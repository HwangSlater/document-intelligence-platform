package com.documentintelligenceapplication.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus processingStatus;

    @Builder
    public Document(String fileName, String filePath, String fileType, Long fileSize, 
                    LocalDateTime uploadDate, String extractedText, ProcessingStatus processingStatus) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadDate = uploadDate;
        this.extractedText = extractedText;
        this.processingStatus = processingStatus;
    }

    public void updateExtractedText(String extractedText) {
        this.extractedText = extractedText;
        this.processingStatus = ProcessingStatus.PARSED;
    }

    public void markAsFailed() {
        this.processingStatus = ProcessingStatus.FAILED;
    }
}
