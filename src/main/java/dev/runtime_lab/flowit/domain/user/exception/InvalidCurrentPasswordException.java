package dev.runtime_lab.flowit.domain.user.exception;

public class InvalidCurrentPasswordException extends RuntimeException {

	public InvalidCurrentPasswordException() {
		super("Invalid current password");
	}
}
