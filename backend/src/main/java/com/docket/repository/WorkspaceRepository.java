package com.docket.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.docket.entity.Workspace;

/**
 * Data access for the workspaces table.
 */
public interface WorkspaceRepository extends JpaRepository<Workspace, Integer> {
}
