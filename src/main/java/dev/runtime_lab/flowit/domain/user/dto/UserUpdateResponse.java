package dev.runtime_lab.flowit.domain.user.dto;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;

public record UserUpdateResponse(
	Long id,
	String email,
	String nickname,
	UserStatus status,
	Long profileImageFileId,
	Long updatedAt
) {

	public static UserUpdateResponse from(User user) {
		Long profileImageFileId = user.getProfileImageFile() == null
			? null
			: user.getProfileImageFile().getId();

		return new UserUpdateResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getStatus(),
			profileImageFileId,
			user.getUpdatedAt()
		);
	}
}
