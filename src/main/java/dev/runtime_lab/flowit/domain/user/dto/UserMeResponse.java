package dev.runtime_lab.flowit.domain.user.dto;

import dev.runtime_lab.flowit.domain.notification.dto.NotificationAlertResponse;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import java.util.List;

public record UserMeResponse(
	Long id,
	String email,
	String nickname,
	UserStatus status,
	Long profileImageFileId,
	String profileImageUrl,
	List<UserMeWorkspaceResponse> workspaces,
	List<NotificationAlertResponse> notificationAlerts
) {

	private static final String PROFILE_IMAGE_URL = "/v1/users/me/profile-image";

	public static String profileImageUrl(Long profileImageFileId) {
		return profileImageFileId == null ? null : PROFILE_IMAGE_URL;
	}
}
