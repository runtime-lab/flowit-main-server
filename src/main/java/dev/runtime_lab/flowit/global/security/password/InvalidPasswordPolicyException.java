package dev.runtime_lab.flowit.global.security.password;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;
import dev.runtime_lab.flowit.global.web.exception.FlowitException;

public class InvalidPasswordPolicyException extends FlowitException {

	public InvalidPasswordPolicyException() {
		super(ErrorCode.VALIDATION_400_001, "Password must not contain special characters");
	}
}
