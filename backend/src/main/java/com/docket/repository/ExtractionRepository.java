package com.docket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.docket.entity.Extraction;

public interface ExtractionRepository extends JpaRepository<Extraction, Long> {
    Optional<Extraction> findByDocumentId(Integer documentId);
}
