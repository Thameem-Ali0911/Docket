package com.docket.service;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.docket.entity.Workspace;
import com.docket.exception.ApiException;
import com.docket.repository.LlmUsageRepository;
import com.docket.repository.WorkspaceRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmBudgetServiceTest {

    @Mock private LlmUsageRepository llmUsageRepository;
    @Mock private WorkspaceRepository workspaceRepository;

    private LlmBudgetService llmBudgetService;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        llmBudgetService = new LlmBudgetService(llmUsageRepository, workspaceRepository);
        workspace = new Workspace("Test Workspace");
    }

    @Test
    @DisplayName("checkAndIncrementBudget increments usage when under budget")
    void testIncrementWhenUnderBudget() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace)); // budget = 100 (default)
        when(llmUsageRepository.getTodayCount(eq(1), any(LocalDate.class))).thenReturn(50);

        assertDoesNotThrow(() -> llmBudgetService.checkAndIncrementBudget(1));

        verify(llmUsageRepository).incrementUsage(eq(1), any(LocalDate.class));
    }

    @Test
    @DisplayName("checkAndIncrementBudget throws 429 when daily budget is exhausted")
    void testThrows429WhenBudgetExhausted() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace)); // budget = 100
        when(llmUsageRepository.getTodayCount(eq(1), any(LocalDate.class))).thenReturn(100);

        ApiException ex = assertThrows(ApiException.class, () ->
                llmBudgetService.checkAndIncrementBudget(1));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertEquals("LLM_BUDGET_EXHAUSTED", ex.getCode());
        verify(llmUsageRepository, never()).incrementUsage(any(), any());
    }

    @Test
    @DisplayName("checkAndIncrementBudget respects custom per-workspace budget")
    void testCustomBudget() {
        workspace.setDailyLlmBudget(5);
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(llmUsageRepository.getTodayCount(eq(1), any(LocalDate.class))).thenReturn(5);

        ApiException ex = assertThrows(ApiException.class, () ->
                llmBudgetService.checkAndIncrementBudget(1));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    @DisplayName("getTodayUsage returns current call count without incrementing")
    void testGetTodayUsage() {
        when(llmUsageRepository.getTodayCount(eq(2), any(LocalDate.class))).thenReturn(42);

        int count = llmBudgetService.getTodayUsage(2);

        assertEquals(42, count);
        verify(llmUsageRepository, never()).incrementUsage(any(), any());
    }
}
