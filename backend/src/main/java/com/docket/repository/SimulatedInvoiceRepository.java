package com.docket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.docket.entity.SimulatedInvoice;

public interface SimulatedInvoiceRepository extends JpaRepository<SimulatedInvoice, Long> {

    List<SimulatedInvoice> findByWorkspaceIdOrderByCreatedAtDesc(Integer workspaceId);
}
