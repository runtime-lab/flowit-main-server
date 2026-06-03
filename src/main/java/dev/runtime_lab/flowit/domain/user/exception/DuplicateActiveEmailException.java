package dev.runtime_lab.flowit.domain.user.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class DuplicateActiveEmailException extends UserException {

	public DuplicateActiveEmailException(String email) {
		super(ErrorCode.USER_409_001, "Active user email already exists: " + email);
	}
}
