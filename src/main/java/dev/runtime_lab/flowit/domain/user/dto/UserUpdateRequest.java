package dev.runtime_lab.flowit.domain.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
	@Size(max = 100)
	String nickname
) {

	@AssertTrue(message = "수정할 사용자 정보가 필요합니다.")
	public boolean isUpdateFieldPresent() {
		return nickname != null;
	}

	@AssertTrue(message = "닉네임은 공백일 수 없습니다.")
	public boolean isNicknameValid() {
		return nickname == null || !nickname.isBlank();
	}
}
