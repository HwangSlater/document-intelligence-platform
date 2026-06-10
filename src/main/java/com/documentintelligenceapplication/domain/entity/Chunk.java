package com.documentintelligenceapplication.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Chunk(Document document, Integer chunkIndex, String content, LocalDateTime createdAt) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.createdAt = createdAt;
    }
}
