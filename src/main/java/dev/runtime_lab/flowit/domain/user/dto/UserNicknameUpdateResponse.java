package dev.runtime_lab.flowit.domain.user.dto;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;

public record UserNicknameUpdateResponse(
	Long id,
	String email,
	String nickname,
	UserStatus status,
	Long profileImageFileId,
	Long updatedAt
) {

	public static UserNicknameUpdateResponse from(User user) {
		Long profileImageFileId = user.getProfileImageFile() == null
			? null
			: user.getProfileImageFile().getId();

		return new UserNicknameUpdateResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getStatus(),
			profileImageFileId,
			user.getUpdatedAt()
		);
	}
}
