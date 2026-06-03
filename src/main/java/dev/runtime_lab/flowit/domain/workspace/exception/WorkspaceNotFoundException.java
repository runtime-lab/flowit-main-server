package dev.runtime_lab.flowit.domain.workspace.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class WorkspaceNotFoundException extends WorkspaceException {

	public WorkspaceNotFoundException() {
		super(ErrorCode.WORKSPACE_404_001, "Workspace not found.");
	}
}
