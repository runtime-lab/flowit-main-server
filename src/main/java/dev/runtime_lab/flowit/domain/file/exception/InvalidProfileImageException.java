package dev.runtime_lab.flowit.domain.file.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class InvalidProfileImageException extends FileException {

	public InvalidProfileImageException(String message) {
		super(ErrorCode.FILE_400_001, message);
	}
}
