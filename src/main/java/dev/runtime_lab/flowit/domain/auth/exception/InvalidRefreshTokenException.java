package dev.runtime_lab.flowit.domain.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException() {
		super("Invalid refresh token");
	}
}
