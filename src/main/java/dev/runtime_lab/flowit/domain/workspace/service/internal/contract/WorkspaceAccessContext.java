package dev.runtime_lab.flowit.domain.workspace.service.internal.contract;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;

public record WorkspaceAccessContext(
	User requester,
	Workspace workspace,
	WorkspaceMember membership
) {
}
