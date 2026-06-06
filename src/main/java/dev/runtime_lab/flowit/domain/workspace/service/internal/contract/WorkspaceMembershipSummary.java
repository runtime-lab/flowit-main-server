package dev.runtime_lab.flowit.domain.workspace.service.internal.contract;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;

public record WorkspaceMembershipSummary(
	Long workspaceId,
	String workspaceName,
	String workspaceDescription,
	Long memberCount,
	WorkspaceMemberRole role,
	Long joinedAt
) {
}
