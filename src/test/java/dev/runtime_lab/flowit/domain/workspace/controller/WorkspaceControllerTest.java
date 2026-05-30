package dev.runtime_lab.flowit.domain.workspace.controller;

import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceCreateService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceControllerTest {

	private final WorkspaceCreateService workspaceCreateService = mock(WorkspaceCreateService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new WorkspaceController(workspaceCreateService))
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

		when(workspaceCreateService.create(any(CurrentUser.class), any(WorkspaceCreateRequest.class)))
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

		verify(workspaceCreateService).create(currentUserCaptor.capture(), requestCaptor.capture());
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
