package dev.runtime_lab.flowit.domain.user.dto;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;

public record UserMeWorkspaceResponse(
	Long id,
	String name,
	String description,
	Long memberCount,
	WorkspaceMemberRole role,
	Long joinedAt
) {
}
