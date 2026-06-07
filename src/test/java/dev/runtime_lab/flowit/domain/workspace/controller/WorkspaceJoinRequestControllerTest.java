package dev.runtime_lab.flowit.domain.workspace.controller;

import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinByInviteCodeRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestHistoryResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinDetailResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestResultResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinDetailsResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestMethod;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import dev.runtime_lab.flowit.domain.workspace.exception.DuplicateWorkspaceMemberException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceInviteCodeNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceJoinRequestService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceJoinRequestControllerTest {

	private final WorkspaceJoinRequestService workspaceJoinRequestService = mock(WorkspaceJoinRequestService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new WorkspaceJoinRequestController(workspaceJoinRequestService))
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
	void joinByInviteCodeCreatesJoinRequestAndMembership() throws Exception {
		String requestBody = """
			{
			  "inviteCode": "A1B2-C3D4-E5F6"
			}
			""";
		WorkspaceJoinRequestResultResponse response = joinedResultResponse();
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<WorkspaceJoinByInviteCodeRequest> requestCaptor =
			ArgumentCaptor.forClass(WorkspaceJoinByInviteCodeRequest.class);

		when(workspaceJoinRequestService.joinByInviteCode(
			any(CurrentUser.class),
			any(WorkspaceJoinByInviteCodeRequest.class)
		)).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces/join-requests/invite-code")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/v1/workspaces/10/join-requests/100"))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.joinRequestId").value(100L))
			.andExpect(jsonPath("$.data.workspaceId").value(10L))
			.andExpect(jsonPath("$.data.memberId").value(300L))
			.andExpect(jsonPath("$.data.method").value("INVITE_CODE"))
			.andExpect(jsonPath("$.data.inviteCode").value("A1B2-C3D4-E5F6"))
			.andExpect(jsonPath("$.data.status").value("JOINED"))
			.andExpect(jsonPath("$.data.joinedAt").value(1779888930L))
			.andExpect(jsonPath("$.data.requestedAt").doesNotExist())
			.andExpect(jsonPath("$.data.readyAt").doesNotExist())
			.andExpect(jsonPath("$.data.approvedAt").doesNotExist())
			.andExpect(jsonPath("$.data.failedAt").doesNotExist())
			.andExpect(jsonPath("$.data.failureCode").doesNotExist())
			.andExpect(jsonPath("$.data.failureMessage").doesNotExist())
			.andExpect(jsonPath("$.data.histories").doesNotExist())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(workspaceJoinRequestService).joinByInviteCode(
			currentUserCaptor.capture(),
			requestCaptor.capture()
		);
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
		assertEquals("A1B2-C3D4-E5F6", requestCaptor.getValue().inviteCode());
	}

	@Test
	void joinByInviteCodeReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(post("/v1/workspaces/join-requests/invite-code")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content("{\"inviteCode\":\"A1B2-C3D4-E5F6\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void joinByInviteCodeRejectsInvalidRequest() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces/join-requests/invite-code")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content("{\"inviteCode\":\"short\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
			.andExpect(jsonPath("$.extensions.fieldErrors").isArray());
	}

	@Test
	void joinByInviteCodeReturnsNotFoundWhenInviteCodeIsMissing() throws Exception {
		when(workspaceJoinRequestService.joinByInviteCode(
			any(CurrentUser.class),
			any(WorkspaceJoinByInviteCodeRequest.class)
		)).thenThrow(new WorkspaceInviteCodeNotFoundException());
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces/join-requests/invite-code")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content("{\"inviteCode\":\"A1B2-C3D4-E5F6\"}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_404_001"))
			.andExpect(jsonPath("$.error.message").value("Workspace invite code not found."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void joinByInviteCodeReturnsConflictWhenUserAlreadyJoined() throws Exception {
		when(workspaceJoinRequestService.joinByInviteCode(
			any(CurrentUser.class),
			any(WorkspaceJoinByInviteCodeRequest.class)
		)).thenThrow(new DuplicateWorkspaceMemberException());
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces/join-requests/invite-code")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content("{\"inviteCode\":\"A1B2-C3D4-E5F6\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_JOIN_REQUEST_409_001"))
			.andExpect(jsonPath("$.error.message").value("User already joined this workspace."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void requestsReturnsWorkspaceJoinRequests() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<Long> workspaceIdCaptor = ArgumentCaptor.forClass(Long.class);
		WorkspaceJoinDetailsResponse response = new WorkspaceJoinDetailsResponse(List.of(joinedResponse()));

		when(workspaceJoinRequestService.requests(any(CurrentUser.class), eq(10L))).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "owner@example.com", "owner"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/join-requests", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.joinRequests[0].joinRequestId").value(100L))
			.andExpect(jsonPath("$.data.joinRequests[0].status").value("JOINED"))
			.andExpect(jsonPath("$.data.joinRequests[0].histories").isArray())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(workspaceJoinRequestService).requests(
			currentUserCaptor.capture(),
			workspaceIdCaptor.capture()
		);
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals(10L, workspaceIdCaptor.getValue());
	}

	@Test
	void requestsReturnsForbiddenWhenRequesterCannotManageJoinRequests() throws Exception {
		when(workspaceJoinRequestService.requests(any(CurrentUser.class), eq(10L)))
			.thenThrow(new WorkspaceMemberAccessDeniedException("Workspace join request history access is not allowed."));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "member@example.com", "member"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/join-requests", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_403_001"))
			.andExpect(jsonPath("$.error.message").value("Workspace join request history access is not allowed."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void requestsReturnsNotFoundWhenWorkspaceIsMissing() throws Exception {
		when(workspaceJoinRequestService.requests(any(CurrentUser.class), eq(10L)))
			.thenThrow(new WorkspaceNotFoundException());
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "owner@example.com", "owner"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/join-requests", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_404_001"))
			.andExpect(jsonPath("$.error.message").value("Workspace not found."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	private WorkspaceJoinRequestResultResponse joinedResultResponse() {
		return new WorkspaceJoinRequestResultResponse(
			100L,
			10L,
			"Flowit",
			1L,
			"member",
			"member@example.com",
			300L,
			WorkspaceJoinRequestMethod.INVITE_CODE,
			"A1B2-C3D4-E5F6",
			WorkspaceJoinRequestStatus.JOINED,
			1779888930L
		);
	}

	private WorkspaceJoinDetailResponse joinedResponse() {
		return new WorkspaceJoinDetailResponse(
			100L,
			10L,
			"Flowit",
			1L,
			"member",
			"member@example.com",
			300L,
			WorkspaceJoinRequestMethod.INVITE_CODE,
			"A1B2-C3D4-E5F6",
			WorkspaceJoinRequestStatus.JOINED,
			1779888900L,
			1779888910L,
			1779888920L,
			1779888930L,
			null,
			null,
			null,
			List.of(
				new WorkspaceJoinRequestHistoryResponse(
					1000L,
					null,
					WorkspaceJoinRequestStatus.PENDING,
					1L,
					1779888900L
				),
				new WorkspaceJoinRequestHistoryResponse(
					1001L,
					WorkspaceJoinRequestStatus.PENDING,
					WorkspaceJoinRequestStatus.READY,
					1L,
					1779888910L
				),
				new WorkspaceJoinRequestHistoryResponse(
					1002L,
					WorkspaceJoinRequestStatus.READY,
					WorkspaceJoinRequestStatus.APPROVED,
					1L,
					1779888920L
				),
				new WorkspaceJoinRequestHistoryResponse(
					1003L,
					WorkspaceJoinRequestStatus.APPROVED,
					WorkspaceJoinRequestStatus.JOINED,
					1L,
					1779888930L
				)
			)
		);
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
}
