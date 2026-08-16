package com.docket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.docket.entity.AnomalyFlag;

public interface AnomalyFlagRepository extends JpaRepository<AnomalyFlag, Long> {
    List<AnomalyFlag> findByDocumentId(Integer documentId);
}
