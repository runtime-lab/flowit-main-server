package dev.runtime_lab.flowit.domain.workspace.dto;

import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;

public record WorkspaceResponse(
	Long id,
	String name,
	String description,
	String inviteCode,
	Long createdAt,
	Long updatedAt
) {

	public static WorkspaceResponse from(Workspace workspace) {
		return new WorkspaceResponse(
			workspace.getId(),
			workspace.getName(),
			workspace.getDescription(),
			workspace.getInviteCode(),
			workspace.getCreatedAt(),
			workspace.getUpdatedAt()
		);
	}
}
