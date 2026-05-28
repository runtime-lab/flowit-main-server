package dev.runtime_lab.flowit.global.security.password;

public class InvalidPasswordPolicyException extends RuntimeException {

	public InvalidPasswordPolicyException() {
		super("Password must not contain special characters");
	}
}
