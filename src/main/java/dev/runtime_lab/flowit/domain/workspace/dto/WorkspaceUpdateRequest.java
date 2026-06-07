package dev.runtime_lab.flowit.domain.workspace.dto;

public record WorkspaceUpdateRequest(
        String name,
        String description
) {
}
