package com.taskflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * TaskAssigneeUpdateRequest — DTO para PATCH /tasks/{id}/assignee.
 * Valida que assigneeId esté presente y sea positivo.
 */
public record TaskAssigneeUpdateRequest(
        @NotNull
        @Positive
        Long assigneeId
) {
}
