package dev.runtime_lab.flowit.domain.file.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class ProfileImageStorageException extends FileException {

	public ProfileImageStorageException(String message) {
		super(ErrorCode.FILE_500_001, message);
	}

	public ProfileImageStorageException(String message, Throwable cause) {
		super(ErrorCode.FILE_500_001, message, cause);
	}
}
