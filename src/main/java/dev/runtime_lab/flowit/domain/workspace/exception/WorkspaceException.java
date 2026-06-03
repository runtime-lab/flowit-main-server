package dev.runtime_lab.flowit.domain.workspace.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;
import dev.runtime_lab.flowit.global.web.exception.FlowitException;

public abstract class WorkspaceException extends FlowitException {

	protected WorkspaceException(ErrorCode errorCode) {
		super(errorCode);
	}

	protected WorkspaceException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
