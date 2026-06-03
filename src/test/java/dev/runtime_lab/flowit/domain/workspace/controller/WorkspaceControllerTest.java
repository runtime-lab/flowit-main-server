package dev.runtime_lab.flowit.domain.workspace.controller;

import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceResponse;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceInviteCodeGenerationException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceControllerTest {

	private final WorkspaceService workspaceService = mock(WorkspaceService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new WorkspaceController(workspaceService))
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
	void createCreatesWorkspace() throws Exception {
		String requestBody = """
			{
			  "name": "Flowit",
			  "description": "Team workspace"
			}
			""";
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<WorkspaceCreateRequest> requestCaptor = ArgumentCaptor.forClass(WorkspaceCreateRequest.class);

		when(workspaceService.create(any(CurrentUser.class), any(WorkspaceCreateRequest.class)))
			.thenReturn(new WorkspaceCreateResponse(10L, 1779889000L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/v1/workspaces/10"))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.createdId").value(10L))
			.andExpect(jsonPath("$.extensions").isMap());

		verify(workspaceService).create(currentUserCaptor.capture(), requestCaptor.capture());
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
		assertEquals("Flowit", requestCaptor.getValue().name());
		assertEquals("Team workspace", requestCaptor.getValue().description());
	}

	@Test
	void createReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		String requestBody = """
			{
			  "name": "Flowit",
			  "description": "Team workspace"
			}
			""";

		mockMvc.perform(post("/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void createRejectsInvalidRequest() throws Exception {
		String requestBody = """
			{
			  "name": "",
			  "description": "%s"
			}
			""".formatted("a".repeat(501));

		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
			.andExpect(jsonPath("$.error.message").value("요청 값이 올바르지 않습니다."))
			.andExpect(jsonPath("$.extensions.fieldErrors").isArray());
	}

	@Test
	void createReturnsBadRequestWhenRequestBodyIsMalformed() throws Exception {
		String requestBody = """
			{
			  "name": "Flowit",
			}
			""";

		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void createReturnsInternalServerErrorWhenInviteCodeGenerationFails() throws Exception {
		String requestBody = """
			{
			  "name": "Flowit",
			  "description": "Team workspace"
			}
			""";

		when(workspaceService.create(any(CurrentUser.class), any(WorkspaceCreateRequest.class)))
			.thenThrow(new WorkspaceInviteCodeGenerationException());
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_500_001"))
			.andExpect(jsonPath("$.error.message").value("워크스페이스 처리에 실패했습니다."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void getReturnsWorkspace() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<Long> workspaceIdCaptor = ArgumentCaptor.forClass(Long.class);
		WorkspaceResponse response = new WorkspaceResponse(
			10L,
			"Flowit",
			"Team workspace",
			"A1B2-C3D4-E5F6",
			1779889000L,
			1779889100L
		);

		when(workspaceService.get(any(CurrentUser.class), eq(10L))).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(10L))
			.andExpect(jsonPath("$.data.name").value("Flowit"))
			.andExpect(jsonPath("$.data.description").value("Team workspace"))
			.andExpect(jsonPath("$.data.inviteCode").value("A1B2-C3D4-E5F6"))
			.andExpect(jsonPath("$.data.createdAt").value(1779889000L))
			.andExpect(jsonPath("$.data.updatedAt").value(1779889100L))
			.andExpect(jsonPath("$.data.createdBy").doesNotExist())
			.andExpect(jsonPath("$.data.deletedAt").doesNotExist())
			.andExpect(jsonPath("$.data.members").doesNotExist())
			.andExpect(jsonPath("$.data.memberCount").doesNotExist())
			.andExpect(jsonPath("$.data.role").doesNotExist())
			.andExpect(jsonPath("$.data.joinedAt").doesNotExist())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(workspaceService).get(currentUserCaptor.capture(), workspaceIdCaptor.capture());
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
		assertEquals(10L, workspaceIdCaptor.getValue());
	}

	@Test
	void getReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(get("/v1/workspaces/{workspaceId}", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void getReturnsForbiddenWhenCurrentUserIsNotWorkspaceMember() throws Exception {
		when(workspaceService.get(any(CurrentUser.class), eq(10L)))
			.thenThrow(new WorkspaceMemberAccessDeniedException("Workspace membership is required."));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_403_001"))
			.andExpect(jsonPath("$.error.message").value("Workspace membership is required."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void getReturnsNotFoundWhenWorkspaceIsMissing() throws Exception {
		when(workspaceService.get(any(CurrentUser.class), eq(10L)))
			.thenThrow(new WorkspaceNotFoundException());
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_404_001"))
			.andExpect(jsonPath("$.error.message").value("Workspace not found."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void getReturnsInternalServerErrorWhenUnexpectedFailureOccurs() throws Exception {
		when(workspaceService.get(any(CurrentUser.class), eq(10L)))
			.thenThrow(new IllegalStateException("database unavailable"));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("INTERNAL_500_001"))
			.andExpect(jsonPath("$.error.message").value("서버 내부 오류가 발생했습니다."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void deleteDeletesWorkspace() throws Exception {
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<Long> workspaceIdCaptor = ArgumentCaptor.forClass(Long.class);

		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(workspaceService).delete(currentUserCaptor.capture(), workspaceIdCaptor.capture());
		assertEquals(1L, currentUserCaptor.getValue().id());
		assertEquals("user@example.com", currentUserCaptor.getValue().email());
		assertEquals("nickname", currentUserCaptor.getValue().name());
		assertEquals(10L, workspaceIdCaptor.getValue());
	}

	@Test
	void deleteReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(delete("/v1/workspaces/{workspaceId}", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid authenticated user."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void deleteReturnsForbiddenWhenCurrentUserIsNotOwner() throws Exception {
		doThrow(new WorkspaceAccessDeniedException())
			.when(workspaceService)
			.delete(any(CurrentUser.class), eq(10L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_403_001"))
			.andExpect(jsonPath("$.error.message").value("Workspace owner permission is required."))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void deleteReturnsNotFoundWhenWorkspaceIsMissing() throws Exception {
		doThrow(new WorkspaceNotFoundException())
			.when(workspaceService)
			.delete(any(CurrentUser.class), eq(10L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}", 10L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WORKSPACE_404_001"))
			.andExpect(jsonPath("$.error.message").value("Workspace not found."))
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
}
