package dev.runtime_lab.flowit.domain.user.controller;

import dev.runtime_lab.flowit.domain.user.dto.UserMeResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserMeWorkspaceResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserPasswordUpdateRequest;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageContentResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageUpdateResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserUpdateRequest;
import dev.runtime_lab.flowit.domain.user.dto.UserUpdateResponse;
import dev.runtime_lab.flowit.domain.user.service.UserMeService;
import dev.runtime_lab.flowit.domain.user.service.UserPasswordUpdateService;
import dev.runtime_lab.flowit.domain.user.service.UserProfileService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUser;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenCookieService;
import dev.runtime_lab.flowit.global.web.response.ApiEmptyData;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserMeService userMeService;
	private final UserProfileService userProfileService;
	private final UserPasswordUpdateService userPasswordUpdateService;
	private final RefreshTokenCookieService refreshTokenCookieService;

	@GetMapping("/me")
	public UserMeResponse me(@AuthenticatedUser CurrentUser currentUser) {
		return userMeService.getMe(currentUser);
	}

	@GetMapping("/me/workspaces")
	public List<UserMeWorkspaceResponse> meWorkspaces(@AuthenticatedUser CurrentUser currentUser) {
		return userMeService.getMeWorkspaces(currentUser);
	}

	@PatchMapping("/me")
	public UserUpdateResponse update(
		@AuthenticatedUser CurrentUser currentUser,
		@Valid @RequestBody UserUpdateRequest request
	) {
		return userProfileService.update(currentUser, request);
	}

	@PatchMapping("/me/password")
	public ResponseEntity<ApiEmptyData> updatePassword(
		@AuthenticatedUser CurrentUser currentUser,
		@Valid @RequestBody UserPasswordUpdateRequest request
	) {
		userPasswordUpdateService.update(currentUser, request);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.expire().toString())
			.body(ApiEmptyData.empty());
	}

	@PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UserProfileImageUpdateResponse replaceProfileImage(
		@AuthenticatedUser CurrentUser currentUser,
		@RequestPart("file") MultipartFile file
	) {
		return userProfileService.replaceProfileImage(currentUser, file);
	}

	@GetMapping(
		value = "/me/profile-image",
		produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE}
	)
	public ResponseEntity<byte[]> getProfileImage(@AuthenticatedUser CurrentUser currentUser) {
		UserProfileImageContentResponse response = userProfileService.getProfileImage(currentUser);

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(response.contentType()))
			.contentLength(response.contentLength())
			.cacheControl(CacheControl.noStore())
			.body(response.bytes());
	}
}
