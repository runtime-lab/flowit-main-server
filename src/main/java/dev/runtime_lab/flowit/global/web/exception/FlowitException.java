package dev.runtime_lab.flowit.global.web.exception;

public abstract class FlowitException extends RuntimeException {

	private final ErrorCode errorCode;
	private final boolean exposeMessage;

	protected FlowitException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage());
	}

	protected FlowitException(ErrorCode errorCode, String message) {
		this(errorCode, message, null);
	}

	protected FlowitException(ErrorCode errorCode, String message, Throwable cause) {
		this(errorCode, message, cause, errorCode.getHttpStatus().is4xxClientError());
	}

	protected FlowitException(ErrorCode errorCode, String message, Throwable cause, boolean exposeMessage) {
		super(message, cause);
		this.errorCode = errorCode;
		this.exposeMessage = exposeMessage;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public boolean isExposeMessage() {
		return exposeMessage;
	}
}
