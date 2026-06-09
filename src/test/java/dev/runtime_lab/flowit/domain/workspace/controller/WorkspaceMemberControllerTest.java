package dev.runtime_lab.flowit.domain.workspace.controller;

import dev.runtime_lab.flowit.domain.file.exception.ProfileImageNotFoundException;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageContentResponse;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberRoleUpdateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMembersResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceMemberService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUserArgumentResolver;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.MEMBERSHIP_REQUIRED;
import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.OWNER_REQUIRED;
import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.ROLE_UPDATE_NOT_ALLOWED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceMemberControllerTest {

	private final WorkspaceMemberService workspaceMemberService = mock(WorkspaceMemberService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new WorkspaceMemberController(workspaceMemberService))
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
	void membersReturnsWorkspaceMembers() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<Long> workspaceIdCaptor = ArgumentCaptor.forClass(Long.class);
		WorkspaceMembersResponse response = new WorkspaceMembersResponse(
			"A1B2-C3D4-E5F6",
			List.of(
				new WorkspaceMemberResponse(
					100L,
					"Owner",
					"owner@example.com",
					UserStatus.ACTIVE,
					WorkspaceMemberRole.OWNER,
					"/v1/workspaces/10/members/100/profile-image"
				)
			)
		);

		when(workspaceMemberService.members(any(CurrentUser.class), eq(10L))).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/members", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.inviteCode").value("A1B2-C3D4-E5F6"))
			.andExpect(jsonPath("$.data.members[0].memberId").value(100L))
			.andExpect(jsonPath("$.data.members[0].name").value("Owner"))
			.andExpect(jsonPath("$.data.members[0].email").value("owner@example.com"))
			.andExpect(jsonPath("$.data.members[0].status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.members[0].role").value("OWNER"))
			.andExpect(jsonPath("$.data.members[0].profileImageUrl")
				.value("/v1/workspaces/10/members/100/profile-image"))
			.andExpect(jsonPath("$.data.members[0].userId").doesNotExist())
			.andExpect(jsonPath("$.data.members[0].joinedAt").doesNotExist())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(workspaceMemberService).members(
			currentUserCaptor.capture(),
			workspaceIdCaptor.capture()
		);
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
		assertEquals(10L, workspaceIdCaptor.getValue());
	}

	@Test
	void membersReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(get("/v1/workspaces/{workspaceId}/members", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void membersReturnsForbiddenWhenRequesterIsNotWorkspaceMember() throws Exception {
		when(workspaceMemberService.members(any(CurrentUser.class), eq(10L)))
			.thenThrow(new WorkspaceMemberAccessDeniedException(MEMBERSHIP_REQUIRED));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/members", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_403_001"))
			.andExpect(jsonPath("$.error.message").value(MEMBERSHIP_REQUIRED))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void membersReturnsNotFoundWhenWorkspaceIsMissing() throws Exception {
		when(workspaceMemberService.members(any(CurrentUser.class), eq(10L)))
			.thenThrow(new WorkspaceNotFoundException());
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/members", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_404_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void getProfileImageReturnsWorkspaceMemberProfileImageContent() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		byte[] content = pngBytes();

		when(workspaceMemberService.getProfileImage(any(CurrentUser.class), eq(10L), eq(100L)))
			.thenReturn(new UserProfileImageContentResponse("image/png", content));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/members/{memberId}/profile-image", 10L, 100L)
				.accept(MediaType.IMAGE_PNG))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.IMAGE_PNG))
			.andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, content.length))
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
			.andExpect(content().bytes(content));

		verify(workspaceMemberService).getProfileImage(
			currentUserCaptor.capture(),
			eq(10L),
			eq(100L)
		);
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
	}

	@Test
	void getProfileImageReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(get("/v1/workspaces/{workspaceId}/members/{memberId}/profile-image", 10L, 100L))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void getProfileImageReturnsForbiddenWhenRequesterIsNotWorkspaceMember() throws Exception {
		when(workspaceMemberService.getProfileImage(any(CurrentUser.class), eq(10L), eq(100L)))
			.thenThrow(new WorkspaceMemberAccessDeniedException(MEMBERSHIP_REQUIRED));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/members/{memberId}/profile-image", 10L, 100L))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_403_001"))
			.andExpect(jsonPath("$.error.message").value(MEMBERSHIP_REQUIRED))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void getProfileImageReturnsNotFoundWhenProfileImageIsMissing() throws Exception {
		when(workspaceMemberService.getProfileImage(any(CurrentUser.class), eq(10L), eq(100L)))
			.thenThrow(new ProfileImageNotFoundException());
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/members/{memberId}/profile-image", 10L, 100L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("FILE_404_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void updateRoleUpdatesWorkspaceMemberRole() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<Long> workspaceIdCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> memberIdCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<WorkspaceMemberRoleUpdateRequest> requestCaptor =
			ArgumentCaptor.forClass(WorkspaceMemberRoleUpdateRequest.class);

		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/workspaces/{workspaceId}/members/{memberId}/role", 10L, 2L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"ADMIN\"}")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(workspaceMemberService).updateRole(
			currentUserCaptor.capture(),
			workspaceIdCaptor.capture(),
			memberIdCaptor.capture(),
			requestCaptor.capture()
		);
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
		assertEquals(10L, workspaceIdCaptor.getValue());
		assertEquals(2L, memberIdCaptor.getValue());
		assertEquals(WorkspaceMemberRole.ADMIN, requestCaptor.getValue().role());
	}

	@Test
	void updateRoleReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(patch("/v1/workspaces/{workspaceId}/members/{memberId}/role", 10L, 2L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"ADMIN\"}")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void updateRoleReturnsForbiddenWhenRoleUpdateIsNotAllowed() throws Exception {
		doThrow(new WorkspaceMemberAccessDeniedException(ROLE_UPDATE_NOT_ALLOWED))
			.when(workspaceMemberService)
			.updateRole(any(CurrentUser.class), eq(10L), eq(2L), any(WorkspaceMemberRoleUpdateRequest.class));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/workspaces/{workspaceId}/members/{memberId}/role", 10L, 2L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"ADMIN\"}")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_403_001"))
			.andExpect(jsonPath("$.error.message").value(ROLE_UPDATE_NOT_ALLOWED))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void removeRemovesWorkspaceMember() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<Long> workspaceIdCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> memberIdCaptor = ArgumentCaptor.forClass(Long.class);

		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/members/{memberId}", 10L, 2L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(workspaceMemberService).remove(
			currentUserCaptor.capture(),
			workspaceIdCaptor.capture(),
			memberIdCaptor.capture()
		);
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
		assertEquals(10L, workspaceIdCaptor.getValue());
		assertEquals(2L, memberIdCaptor.getValue());
	}

	@Test
	void removeReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/members/{memberId}", 10L, 2L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void removeReturnsForbiddenWhenRemovalIsNotAllowed() throws Exception {
		doThrow(new WorkspaceMemberAccessDeniedException())
			.when(workspaceMemberService)
			.remove(any(CurrentUser.class), eq(10L), eq(2L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/members/{memberId}", 10L, 2L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_403_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void removeReturnsNotFoundWhenWorkspaceIsMissing() throws Exception {
		doThrow(new WorkspaceNotFoundException())
			.when(workspaceMemberService)
			.remove(any(CurrentUser.class), eq(10L), eq(2L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/members/{memberId}", 10L, 2L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_404_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void removeReturnsNotFoundWhenTargetMemberIsMissing() throws Exception {
		doThrow(new WorkspaceMemberNotFoundException())
			.when(workspaceMemberService)
			.remove(any(CurrentUser.class), eq(10L), eq(2L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/members/{memberId}", 10L, 2L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_MEMBER_404_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void withdrawWithdrawsCurrentWorkspaceMember() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<Long> workspaceIdCaptor = ArgumentCaptor.forClass(Long.class);

		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/members/withdraw", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(workspaceMemberService).withdraw(
			currentUserCaptor.capture(),
			workspaceIdCaptor.capture()
		);
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
		assertEquals(10L, workspaceIdCaptor.getValue());
	}

	@Test
	void withdrawReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/members/withdraw", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void withdrawReturnsForbiddenWhenWithdrawalIsNotAllowed() throws Exception {
		doThrow(new WorkspaceMemberAccessDeniedException(OWNER_REQUIRED))
			.when(workspaceMemberService)
			.withdraw(any(CurrentUser.class), eq(10L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/members/withdraw", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_403_001"))
			.andExpect(jsonPath("$.error.message").value(OWNER_REQUIRED))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void withdrawReturnsNotFoundWhenWorkspaceIsMissing() throws Exception {
		doThrow(new WorkspaceNotFoundException())
			.when(workspaceMemberService)
			.withdraw(any(CurrentUser.class), eq(10L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/members/withdraw", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_404_001"))
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
