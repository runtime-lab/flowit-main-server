package dev.runtime_lab.flowit.domain.user.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;
import dev.runtime_lab.flowit.global.web.exception.FlowitException;

public abstract class UserException extends FlowitException {

	protected UserException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
