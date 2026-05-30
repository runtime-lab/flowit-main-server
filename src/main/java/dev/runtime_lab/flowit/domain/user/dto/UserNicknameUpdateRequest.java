package dev.runtime_lab.flowit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserNicknameUpdateRequest(
	@NotBlank
	@Size(max = 100)
	String nickname
) {
}
