package com.docket.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docket.entity.Workspace;
import com.docket.exception.ApiException;
import com.docket.repository.LlmUsageRepository;
import com.docket.repository.WorkspaceRepository;

/**
 * Guards against denial-of-wallet attacks by enforcing a per-workspace daily
 * LLM invocation budget. Each LLM call (extraction, summarization, anomaly check)
 * must call {@link #checkAndIncrementBudget(Integer)} before invoking the Gemini API.
 *
 * <p>Budget defaults to 100 calls/day per workspace (configurable via the
 * {@code workspaces.daily_llm_budget} column or application property fallback).</p>
 */
@Service
public class LlmBudgetService {

    private static final Logger log = LoggerFactory.getLogger(LlmBudgetService.class);

    /** Hard-coded global safety ceiling — prevents any single workspace from calling more
     *  than this even if the DB row has a higher value. */
    private static final int GLOBAL_CEILING = 500;

    private final LlmUsageRepository llmUsageRepository;
    private final WorkspaceRepository workspaceRepository;

    public LlmBudgetService(LlmUsageRepository llmUsageRepository,
                             WorkspaceRepository workspaceRepository) {
        this.llmUsageRepository = llmUsageRepository;
        this.workspaceRepository = workspaceRepository;
    }

    /**
     * Checks whether the workspace has remaining LLM budget for today and, if so,
     * atomically increments the usage counter.
     *
     * @param workspaceId the workspace to check and charge
     * @throws ApiException HTTP 429 if the daily budget is exhausted
     */
    @Transactional
    public void checkAndIncrementBudget(Integer workspaceId) {
        LocalDate today = LocalDate.now();
        int currentCount = llmUsageRepository.getTodayCount(workspaceId, today);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "Workspace not found"));

        int budget = Math.min(workspace.getDailyLlmBudget(), GLOBAL_CEILING);

        if (currentCount >= budget) {
            log.warn("LLM budget exhausted for workspace {} — {} calls used today (limit={})",
                    workspaceId, currentCount, budget);
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "LLM_BUDGET_EXHAUSTED",
                    "Daily LLM budget exhausted. Your workspace is limited to " + budget +
                    " AI operations per day. Try again tomorrow.");
        }

        llmUsageRepository.incrementUsage(workspaceId, today);
        log.debug("LLM budget charged for workspace {} — {} / {}", workspaceId, currentCount + 1, budget);
    }

    /**
     * Returns current daily usage for a workspace without charging it.
     *
     * @param workspaceId the workspace to inspect
     * @return number of LLM calls made today
     */
    public int getTodayUsage(Integer workspaceId) {
        return llmUsageRepository.getTodayCount(workspaceId, LocalDate.now());
    }
}
