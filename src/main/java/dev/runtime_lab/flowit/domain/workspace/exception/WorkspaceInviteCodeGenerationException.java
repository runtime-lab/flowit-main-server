package dev.runtime_lab.flowit.domain.workspace.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class WorkspaceInviteCodeGenerationException extends WorkspaceException {

	public WorkspaceInviteCodeGenerationException() {
		super(ErrorCode.WORKSPACE_500_001, "Could not generate a unique workspace invite code.");
	}
}
