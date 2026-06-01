package dev.runtime_lab.flowit.domain.user.controller;

import dev.runtime_lab.flowit.domain.file.exception.InvalidProfileImageException;
import dev.runtime_lab.flowit.domain.file.exception.ProfileImageNotFoundException;
import dev.runtime_lab.flowit.domain.user.dto.UserMeResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserMeWorkspaceResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserNicknameUpdateRequest;
import dev.runtime_lab.flowit.domain.user.dto.UserNicknameUpdateResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserPasswordUpdateRequest;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageContentResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageUpdateResponse;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.service.UserMeService;
import dev.runtime_lab.flowit.domain.user.service.UserPasswordUpdateService;
import dev.runtime_lab.flowit.domain.user.service.UserProfileService;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUserArgumentResolver;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenCookieService;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

	private final UserMeService userMeService = mock(UserMeService.class);
	private final UserProfileService userProfileService = mock(UserProfileService.class);
	private final UserPasswordUpdateService userPasswordUpdateService = mock(UserPasswordUpdateService.class);
	private final RefreshTokenCookieService refreshTokenCookieService = new RefreshTokenCookieService(
		new JwtProperties(
			"flowit-test",
			Duration.ofMinutes(15),
			Duration.ofDays(14),
			"flowit_refresh_token",
			"/v1/public/auth",
			"Lax",
			false,
			null,
			null,
			null,
			null
		)
	);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(
				userMeService,
				userProfileService,
				userPasswordUpdateService,
				refreshTokenCookieService
			))
			.setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
			.setControllerAdvice(new ApiResponseBodyAdvice(), new GlobalExceptionHandler())
			.setValidator(validator)
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void meReturnsCurrentUserWithWorkspaces() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		UserMeResponse response = new UserMeResponse(
			1L,
			"user@example.com",
			"nickname",
			UserStatus.ACTIVE,
			3001L,
			"/v1/users/me/profile-image",
			List.of(new UserMeWorkspaceResponse(10L, "Flowit", "Team workspace", 3L, WorkspaceMemberRole.OWNER, 2L)),
			List.of()
		);

		when(userMeService.getMe(any(CurrentUser.class))).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/users/me")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(1L))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("nickname"))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.profileImageFileId").value(3001L))
			.andExpect(jsonPath("$.data.profileImageUrl").value("/v1/users/me/profile-image"))
			.andExpect(jsonPath("$.data.workspaces[0].id").value(10L))
			.andExpect(jsonPath("$.data.workspaces[0].name").value("Flowit"))
			.andExpect(jsonPath("$.data.workspaces[0].description").value("Team workspace"))
			.andExpect(jsonPath("$.data.workspaces[0].memberCount").value(3L))
			.andExpect(jsonPath("$.data.workspaces[0].role").value("OWNER"))
			.andExpect(jsonPath("$.data.workspaces[0].joinedAt").value(2L))
			.andExpect(jsonPath("$.data.notificationAlerts").isArray())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(userMeService).getMe(currentUserCaptor.capture());
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
	}

	@Test
	void meReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(get("/v1/users/me")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void updateNicknameUpdatesCurrentUserNickname() throws Exception {
		String requestBody = """
			{
			  "nickname": "new-nickname"
			}
			""";
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<UserNicknameUpdateRequest> requestCaptor = ArgumentCaptor.forClass(UserNicknameUpdateRequest.class);

		when(userProfileService.updateNickname(any(CurrentUser.class), any(UserNicknameUpdateRequest.class)))
			.thenReturn(new UserNicknameUpdateResponse(1L, "user@example.com", "new-nickname", UserStatus.ACTIVE, null, 3L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/users/me/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(1L))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("new-nickname"))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.updatedAt").value(3L))
			.andExpect(jsonPath("$.extensions").isMap());

		verify(userProfileService).updateNickname(currentUserCaptor.capture(), requestCaptor.capture());
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("new-nickname", requestCaptor.getValue().nickname());
	}

	@Test
	void updateNicknameReturnsBadRequestWhenNicknameIsBlank() throws Exception {
		String requestBody = """
			{
			  "nickname": ""
			}
			""";
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/users/me/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
			.andExpect(jsonPath("$.extensions.fieldErrors").isArray());
	}

	@Test
	void updateNicknameReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		String requestBody = """
			{
			  "nickname": "new-nickname"
			}
			""";

		mockMvc.perform(patch("/v1/users/me/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void updatePasswordChangesCurrentUserPasswordAndExpiresRefreshCookie() throws Exception {
		String requestBody = """
			{
			  "currentPassword": "oldPassword",
			  "newPassword": "newPassword"
			}
			""";
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<UserPasswordUpdateRequest> requestCaptor = ArgumentCaptor.forClass(UserPasswordUpdateRequest.class);

		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/users/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
				containsString("flowit_refresh_token="),
				containsString("Path=/v1/public/auth"),
				containsString("Max-Age=0"),
				containsString("HttpOnly"),
				containsString("SameSite=Lax")
			)))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(userPasswordUpdateService).update(currentUserCaptor.capture(), requestCaptor.capture());
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("oldPassword", requestCaptor.getValue().currentPassword());
		assertEquals("newPassword", requestCaptor.getValue().newPassword());
	}

	@Test
	void updatePasswordReturnsBadRequestWhenNewPasswordIsTooShort() throws Exception {
		String requestBody = """
			{
			  "currentPassword": "oldPassword",
			  "newPassword": "short"
			}
			""";
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/users/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
			.andExpect(jsonPath("$.extensions.fieldErrors").isArray());
	}

	@Test
	void updatePasswordReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		String requestBody = """
			{
			  "currentPassword": "oldPassword",
			  "newPassword": "newPassword"
			}
			""";

		mockMvc.perform(patch("/v1/users/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void replaceProfileImageUpdatesCurrentUserProfileImage() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);

		when(userProfileService.replaceProfileImage(any(CurrentUser.class), any()))
			.thenReturn(new UserProfileImageUpdateResponse(3001L, "image/png", 68L, 1, 1));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(multipart("/v1/users/me/profile-image")
				.file("file", pngBytes())
				.with(request -> {
					request.setMethod("PUT");
					return request;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.fileId").value(3001L))
			.andExpect(jsonPath("$.data.contentType").value("image/png"))
			.andExpect(jsonPath("$.data.width").value(1))
			.andExpect(jsonPath("$.data.height").value(1))
			.andExpect(jsonPath("$.extensions").isMap());

		verify(userProfileService).replaceProfileImage(currentUserCaptor.capture(), any());
		assertEquals(1L, currentUserCaptor.getValue().id());
	}

	@Test
	void replaceProfileImageReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(multipart("/v1/users/me/profile-image")
				.file("file", pngBytes())
				.with(request -> {
					request.setMethod("PUT");
					return request;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void replaceProfileImageReturnsBadRequestWhenImageIsInvalid() throws Exception {
		when(userProfileService.replaceProfileImage(any(CurrentUser.class), any()))
			.thenThrow(new InvalidProfileImageException("지원하지 않는 프로필 이미지 MIME type입니다."));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(multipart("/v1/users/me/profile-image")
				.file("file", "not-image".getBytes(java.nio.charset.StandardCharsets.UTF_8))
				.with(request -> {
					request.setMethod("PUT");
					return request;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("FILE_400_001"))
			.andExpect(jsonPath("$.error.message").value("지원하지 않는 프로필 이미지 MIME type입니다."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void getProfileImageReturnsCurrentUserProfileImageContent() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		byte[] content = pngBytes();

		when(userProfileService.getProfileImage(any(CurrentUser.class)))
			.thenReturn(new UserProfileImageContentResponse("image/png", content));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/users/me/profile-image")
				.accept(MediaType.IMAGE_PNG))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.IMAGE_PNG))
			.andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, content.length))
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
			.andExpect(content().bytes(content));

		verify(userProfileService).getProfileImage(currentUserCaptor.capture());
		assertEquals(1L, currentUserCaptor.getValue().id());
	}

	@Test
	void getProfileImageReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(get("/v1/users/me/profile-image"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void getProfileImageReturnsNotFoundWhenProfileImageIsMissing() throws Exception {
		when(userProfileService.getProfileImage(any(CurrentUser.class)))
			.thenThrow(new ProfileImageNotFoundException());
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/users/me/profile-image"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("FILE_404_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	private Jwt jwt(String subject, String email, String name) {
		return Jwt.withTokenValue("token")
			.header("alg", "none")
			.subject(subject)
			.claim("email", email)
			.claim("name", name)
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
			.build();
	}

	private byte[] pngBytes() {
		return java.util.Base64.getDecoder().decode(
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
		);
	}
}
