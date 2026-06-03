package dev.runtime_lab.flowit.domain.workspace.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class WorkspaceMemberNotFoundException extends WorkspaceException {

	public WorkspaceMemberNotFoundException() {
		super(ErrorCode.WORKSPACE_MEMBER_404_001, "Workspace member not found.");
	}
}
