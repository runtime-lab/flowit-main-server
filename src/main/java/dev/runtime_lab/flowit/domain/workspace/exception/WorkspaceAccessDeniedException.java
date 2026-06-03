package dev.runtime_lab.flowit.domain.workspace.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class WorkspaceAccessDeniedException extends WorkspaceException {

	public WorkspaceAccessDeniedException() {
		super(ErrorCode.AUTH_403_001, "Workspace owner permission is required.");
	}
}
