package dev.runtime_lab.flowit.domain.workspace.exception;

public class WorkspaceMemberAccessDeniedException extends RuntimeException {

	public WorkspaceMemberAccessDeniedException() {
		super("Workspace member removal is not allowed.");
	}

	public WorkspaceMemberAccessDeniedException(String message) {
		super(message);
	}
}
