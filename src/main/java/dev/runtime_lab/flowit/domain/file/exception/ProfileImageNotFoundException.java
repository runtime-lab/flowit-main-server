package dev.runtime_lab.flowit.domain.file.exception;

public class ProfileImageNotFoundException extends RuntimeException {

	public ProfileImageNotFoundException() {
		this("등록된 프로필 이미지를 찾을 수 없습니다.");
	}

	public ProfileImageNotFoundException(String message) {
		super(message);
	}
}
