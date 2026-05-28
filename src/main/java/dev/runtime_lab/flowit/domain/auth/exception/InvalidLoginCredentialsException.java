package dev.runtime_lab.flowit.domain.auth.exception;

public class InvalidLoginCredentialsException extends RuntimeException {

	public InvalidLoginCredentialsException() {
		super("Invalid email or password");
	}
}
