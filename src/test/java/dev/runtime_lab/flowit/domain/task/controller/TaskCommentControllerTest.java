package dev.runtime_lab.flowit.domain.task.controller;

import dev.runtime_lab.flowit.domain.task.dto.TaskCommentAuthorResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentUpdateRequest;
import dev.runtime_lab.flowit.domain.task.service.TaskCommentService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUserArgumentResolver;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiListData;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskCommentControllerTest {

	private final TaskCommentService taskCommentService = mock(TaskCommentService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new TaskCommentController(taskCommentService))
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
	void createCreatesTaskComment() throws Exception {
		String requestBody = """
			{
			  "contentMarkdown": "확인했습니다."
			}
			""";
		ArgumentCaptor<TaskCommentCreateRequest> requestCaptor =
			ArgumentCaptor.forClass(TaskCommentCreateRequest.class);

		when(taskCommentService.create(any(CurrentUser.class), eq(1L), eq(100L), any(TaskCommentCreateRequest.class)))
			.thenReturn(new TaskCommentCreateResponse(500L, 1780916400L));
		authenticate();

		mockMvc.perform(post("/v1/workspaces/{workspaceId}/tasks/{taskId}/comments", 1L, 100L)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/v1/workspaces/1/tasks/100/comments/500"))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.createdId").value(500L))
			.andExpect(jsonPath("$.extensions").isMap());

		verify(taskCommentService).create(any(CurrentUser.class), eq(1L), eq(100L), requestCaptor.capture());
		assertEquals("확인했습니다.", requestCaptor.getValue().contentMarkdown());
	}

	@Test
	void commentsBindsPagination() throws Exception {
		TaskCommentResponse comment = new TaskCommentResponse(
			500L,
			100L,
			new TaskCommentAuthorResponse(10L, "Actor"),
			"수정된 댓글입니다.",
			true,
			true,
			true,
			1780916400L,
			1780920000L
		);

		when(taskCommentService.comments(any(CurrentUser.class), eq(1L), eq(100L), eq(0), eq(20)))
			.thenReturn(ApiListData.of(List.of(comment), 1L));
		authenticate();

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/tasks/{taskId}/comments", 1L, 100L)
				.param("page", "0")
				.param("size", "20")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.items[0].id").value(500L))
			.andExpect(jsonPath("$.data.items[0].edited").value(true))
			.andExpect(jsonPath("$.data.items[0].editable").value(true))
			.andExpect(jsonPath("$.data.items[0].ownedByRequester").value(true))
			.andExpect(jsonPath("$.data.items[0].author.userId").doesNotExist())
			.andExpect(jsonPath("$.data.totalCount").value(1L));

		verify(taskCommentService).comments(any(CurrentUser.class), eq(1L), eq(100L), eq(0), eq(20));
	}

	@Test
	void updateUpdatesTaskComment() throws Exception {
		String requestBody = """
			{
			  "contentMarkdown": "수정했습니다."
			}
			""";
		ArgumentCaptor<TaskCommentUpdateRequest> requestCaptor =
			ArgumentCaptor.forClass(TaskCommentUpdateRequest.class);
		authenticate();

		mockMvc.perform(patch("/v1/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}", 1L, 100L, 500L)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isMap());

		verify(taskCommentService)
			.update(any(CurrentUser.class), eq(1L), eq(100L), eq(500L), requestCaptor.capture());
		assertEquals("수정했습니다.", requestCaptor.getValue().contentMarkdown());
	}

	@Test
	void deleteDeletesTaskComment() throws Exception {
		authenticate();

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}", 1L, 100L, 500L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isMap());

		verify(taskCommentService).delete(any(CurrentUser.class), eq(1L), eq(100L), eq(500L));
	}

	private void authenticate() {
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1", "actor@example.com", "Actor"), List.of())
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
