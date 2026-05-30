package dev.runtime_lab.flowit.domain.workspace.exception;

public class WorkspaceInviteCodeGenerationException extends RuntimeException {

	public WorkspaceInviteCodeGenerationException() {
		super("Could not generate a unique workspace invite code.");
	}
}
