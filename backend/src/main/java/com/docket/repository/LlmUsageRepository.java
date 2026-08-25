package com.docket.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.docket.entity.LlmUsage;

public interface LlmUsageRepository extends JpaRepository<LlmUsage, Long> {

    Optional<LlmUsage> findByWorkspaceIdAndUsageDate(Integer workspaceId, LocalDate date);

    /**
     * Atomically increments or inserts the usage row for today.
     * Uses PostgreSQL upsert to avoid race conditions under concurrent uploads.
     */
    @Modifying
    @Query(value = """
            INSERT INTO llm_usage (workspace_id, usage_date, call_count)
            VALUES (:workspaceId, :date, 1)
            ON CONFLICT (workspace_id, usage_date)
            DO UPDATE SET call_count = llm_usage.call_count + 1
            """, nativeQuery = true)
    void incrementUsage(@Param("workspaceId") Integer workspaceId,
                        @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(u.callCount), 0) FROM LlmUsage u WHERE u.workspace.id = :workspaceId AND u.usageDate = :date")
    int getTodayCount(@Param("workspaceId") Integer workspaceId, @Param("date") LocalDate date);
}
