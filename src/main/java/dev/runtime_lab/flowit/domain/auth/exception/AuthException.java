package dev.runtime_lab.flowit.domain.auth.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;
import dev.runtime_lab.flowit.global.web.exception.FlowitException;

public abstract class AuthException extends FlowitException {

	protected AuthException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
