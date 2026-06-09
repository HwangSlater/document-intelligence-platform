package com.documentintelligenceapplication.domain.repository;

import com.documentintelligenceapplication.domain.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, UUID> {
}
