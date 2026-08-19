package com.docket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.docket.entity.Summary;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    Optional<Summary> findByDocumentId(Integer documentId);
    List<Summary> findByDocumentIdIn(List<Integer> documentIds);
}

