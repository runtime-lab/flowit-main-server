package dev.runtime_lab.flowit.domain.workspace.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkspaceAccessMessages {

	public static final String MEMBERSHIP_REQUIRED = "Workspace membership is required.";
	public static final String OWNER_REQUIRED = "Workspace must have at least one owner.";
	public static final String ROLE_UPDATE_NOT_ALLOWED = "Workspace member role update is not allowed.";
	public static final String JOIN_REQUEST_HISTORY_ACCESS_DENIED =
		"Workspace join request history access is not allowed.";
	public static final String WORKSPACE_UPDATE_NOT_ALLOWED = "Workspace update is not allowed.";
}
