package dev.runtime_lab.flowit.domain.workspace.dto;

import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;

public record WorkspaceCreateResponse(
	Long id,
	Long createdAt
) {

	public static WorkspaceCreateResponse from(Workspace workspace) {
		return new WorkspaceCreateResponse(
			workspace.getId(),
			workspace.getCreatedAt()
		);
	}
}
