package dev.runtime_lab.flowit.domain.task.controller;

import dev.runtime_lab.flowit.domain.task.dto.TaskCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskListQuery;
import dev.runtime_lab.flowit.domain.task.dto.TaskSummaryResponse;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import dev.runtime_lab.flowit.domain.task.service.TaskService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerTest {

	private final TaskService taskService = mock(TaskService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new TaskController(taskService))
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
	void createCreatesTask() throws Exception {
		String requestBody = """
			{
			  "title": "로그인 UI 구현",
			  "descriptionMarkdown": "### 로그인 화면",
			  "status": "TO_DO",
			  "assigneeMemberId": 12,
			  "priority": "HIGH",
			  "startDate": 1780876800,
			  "dueDate": 1781222400,
			  "tags": ["frontend", "ui"]
			}
			""";
		ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
		ArgumentCaptor<TaskCreateRequest> requestCaptor = ArgumentCaptor.forClass(TaskCreateRequest.class);

		when(taskService.create(any(CurrentUser.class), eq(1L), any(TaskCreateRequest.class)))
			.thenReturn(new TaskCreateResponse(100L, 1780916400L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("7", "user@example.com", "김철수"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces/{workspaceId}/tasks", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/v1/workspaces/1/tasks/100"))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.createdId").value(100L))
			.andExpect(jsonPath("$.extensions").isMap());

		verify(taskService).create(currentUserCaptor.capture(), eq(1L), requestCaptor.capture());
		assertEquals(7L, currentUserCaptor.getValue().id());
		assertEquals("로그인 UI 구현", requestCaptor.getValue().title());
		assertEquals(12L, requestCaptor.getValue().assigneeMemberId());
	}

	@Test
	void tasksBindsSearchRequest() throws Exception {
		ArgumentCaptor<TaskListQuery> queryCaptor = ArgumentCaptor.forClass(TaskListQuery.class);

		when(taskService.tasks(any(CurrentUser.class), eq(1L), any(TaskListQuery.class)))
			.thenReturn(ApiListData.of(List.<TaskSummaryResponse>of(), 0L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("7", "user@example.com", "김철수"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/tasks", 1L)
				.param("status", "IN_PROGRESS")
				.param("assigneeMemberId", "12")
				.param("tag", "frontend")
				.param("keyword", "login")
				.param("dueFrom", "1780876800")
				.param("dueTo", "1781222400")
				.param("page", "2")
				.param("size", "30")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.items").isArray())
			.andExpect(jsonPath("$.data.totalCount").value(0L));

		verify(taskService).tasks(any(CurrentUser.class), eq(1L), queryCaptor.capture());
		TaskListQuery query = queryCaptor.getValue();
		assertEquals(TaskStatus.IN_PROGRESS, query.status());
		assertEquals(12L, query.assigneeMemberId());
		assertEquals("frontend", query.tag());
		assertEquals("login", query.keyword());
		assertEquals(1780876800L, query.dueFrom());
		assertEquals(1781222400L, query.dueTo());
		assertEquals(2, query.page());
		assertEquals(30, query.size());
	}

	@Test
	void tasksRejectsInvalidSearchRequest() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("7", "user@example.com", "김철수"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/tasks", 1L)
				.param("page", "-1")
				.param("size", "201")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"));
	}

	@Test
	void updateProgressRejectsOutOfRangeProgress() throws Exception {
		String requestBody = """
			{
			  "progress": 101
			}
			""";

		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("7", "user@example.com", "김철수"), List.of())
		);

		mockMvc.perform(patch("/v1/workspaces/{workspaceId}/tasks/{taskId}/progress", 1L, 100L)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
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
