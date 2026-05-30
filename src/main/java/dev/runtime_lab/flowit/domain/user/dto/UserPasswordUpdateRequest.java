package dev.runtime_lab.flowit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
	@NotBlank
	@Size(max = 72)
	String currentPassword,

	@NotBlank
	@Size(min = 8, max = 24)
	String newPassword
) {
}
