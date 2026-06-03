package dev.runtime_lab.flowit.global.security.authentication;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;
import dev.runtime_lab.flowit.global.web.exception.FlowitException;

public class InvalidAuthenticatedUserException extends FlowitException {

	public InvalidAuthenticatedUserException() {
		super(ErrorCode.AUTH_401_001, "Invalid authenticated user.");
	}
}
