package dev.runtime_lab.flowit.domain.file.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class ProfileImageNotFoundException extends FileException {

	public ProfileImageNotFoundException() {
		this("등록된 프로필 이미지를 찾을 수 없습니다.");
	}

	public ProfileImageNotFoundException(String message) {
		super(ErrorCode.FILE_404_001, message);
	}
}
