package dev.runtime_lab.flowit.domain.auth.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class InvalidLoginCredentialsException extends AuthException {

	public InvalidLoginCredentialsException() {
		super(ErrorCode.AUTH_401_001, "Invalid email or password");
	}
}
