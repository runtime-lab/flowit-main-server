package dev.runtime_lab.flowit.domain.file.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;
import dev.runtime_lab.flowit.global.web.exception.FlowitException;

public abstract class FileException extends FlowitException {

	protected FileException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	protected FileException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, cause);
	}
}
