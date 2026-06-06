package dev.runtime_lab.flowit.domain.workspace.repository.projection;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;

public record WorkspaceMembershipProjection(
	Long workspaceId,
	String workspaceName,
	String workspaceDescription,
	Long memberCount,
	WorkspaceMemberRole role,
	Long joinedAt
) {
}
