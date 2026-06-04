package dev.runtime_lab.flowit.domain.workspace.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class WorkspaceInviteCodeNotFoundException extends WorkspaceException {

	public WorkspaceInviteCodeNotFoundException() {
		super(ErrorCode.WORKSPACE_404_001, "Workspace invite code not found.");
	}
}
