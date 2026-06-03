package dev.runtime_lab.flowit.domain.workspace.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class WorkspaceMemberAccessDeniedException extends WorkspaceException {

	public WorkspaceMemberAccessDeniedException() {
		super(ErrorCode.AUTH_403_001, "Workspace member removal is not allowed.");
	}

	public WorkspaceMemberAccessDeniedException(String message) {
		super(ErrorCode.AUTH_403_001, message);
	}
}
