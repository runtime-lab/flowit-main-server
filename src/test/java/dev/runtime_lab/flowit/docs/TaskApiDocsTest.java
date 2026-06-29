package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.task.controller.TaskController;
import dev.runtime_lab.flowit.domain.task.dto.TaskAssigneeResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentAuthorResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskDetailResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskHistoryActorResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskHistoryChangeResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskHistoryResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskHistoryTargetResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskListQuery;
import dev.runtime_lab.flowit.domain.task.dto.TaskSummaryResponse;
import dev.runtime_lab.flowit.domain.task.entity.TaskHistoryAction;
import dev.runtime_lab.flowit.domain.task.entity.TaskHistoryElement;
import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static dev.runtime_lab.flowit.docs.support.DocumentedTypes.numberParameter;
import static dev.runtime_lab.flowit.docs.support.DocumentedTypes.stringArrayElements;
import static dev.runtime_lab.flowit.docs.support.DocumentedTypes.stringParameter;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class TaskApiDocsTest {

	private static final Long START_DATE = 1780876800L;
	private static final Long DUE_DATE = 1781222400L;

	private final TaskService taskService = mock(TaskService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new TaskController(taskService))
			.setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
			.setControllerAdvice(new ApiResponseBodyAdvice(), new GlobalExceptionHandler())
			.setValidator(validator)
			.apply(documentationConfiguration(restDocumentation))
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createTask() throws Exception {
		String requestBody = """
			{
			  "title": "Login UI",
			  "descriptionMarkdown": "### Login screen\\n- Add email field",
			  "status": "TODO",
			  "assigneeMemberId": 3002,
			  "priority": "HIGH",
			  "startDate": 1780876800,
			  "dueDate": 1781222400,
			  "progress": 35,
			  "tags": ["frontend", "ui"]
			}
			""";

		when(taskService.create(any(CurrentUser.class), eq(2001L), any(TaskCreateRequest.class)))
			.thenReturn(new TaskCreateResponse(1001L, 1780916400L));
		authenticate();

		mockMvc.perform(post("/v1/workspaces/{workspaceId}/tasks", 2001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andDo(document("workspaces-tasks-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				workspacePathParameters("작업을 생성할 워크스페이스 식별자입니다."),
				authRequestHeaders(),
				requestFields(taskCreateRequestFields()),
				responseHeaders(
					headerWithName(HttpHeaders.LOCATION).description("생성된 작업 상세 조회 리소스 위치입니다."),
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.")
				),
				responseFields(createdResponseFields("생성된 작업 식별자입니다."))
			));
	}

	@Test
	void listTasks() throws Exception {
		TaskAssigneeResponse assignee = new TaskAssigneeResponse(3002L, 1002L, "Assignee", "assignee@example.com");
		List<TaskSummaryResponse> response = List.of(
			new TaskSummaryResponse(
				1001L,
				2001L,
				"Login UI",
				TaskStatus.IN_PROGRESS,
				assignee,
				TaskPriority.HIGH,
				START_DATE,
				DUE_DATE,
				List.of("frontend", "ui"),
				35,
				1780916400L,
				1780920000L
			)
		);

		when(taskService.tasks(any(CurrentUser.class), eq(2001L), any(TaskListQuery.class)))
			.thenReturn(ApiListData.of(response, 1L));
		authenticate();

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/tasks", 2001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.param("status", "IN_PROGRESS")
				.param("assigneeMemberId", "3002")
				.param("tag", "frontend")
				.param("keyword", "login")
				.param("dueFrom", "1780876800")
				.param("dueTo", "1781222400")
				.param("page", "0")
				.param("size", "20")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("workspaces-tasks-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				workspacePathParameters("작업 목록을 조회할 워크스페이스 식별자입니다."),
				authRequestHeaders(),
				queryParameters(
					stringParameter("status").description("작업 상태 필터입니다. link:workspaces-tasks-enum-reference.html#task-status[TaskStatus]를 참고합니다.").optional(),
					numberParameter("assigneeMemberId").description("담당 워크스페이스 멤버 식별자 필터입니다. 미할당 작업만 조회하는 필터는 아직 제공하지 않습니다.").optional(),
					stringParameter("tag").description("태그 이름 필터입니다. 서버는 정규화된 태그 이름으로 검색합니다.").optional(),
					stringParameter("keyword").description("작업 제목/설명 검색 키워드입니다.").optional(),
					numberParameter("dueFrom").description("마감 예정일 검색 시작 시각입니다. Unix epoch seconds 기준입니다.").optional(),
					numberParameter("dueTo").description("마감 예정일 검색 종료 시각입니다. Unix epoch seconds 기준입니다.").optional(),
					numberParameter("page").description("0부터 시작하는 페이지 번호입니다. 생략 시 서버 기본값을 사용합니다.").optional(),
					numberParameter("size").description("페이지 크기입니다. 생략 시 서버 기본값을 사용합니다.").optional()
				),
				responseHeaders(contentTypeResponseHeader()),
				responseFields(taskListResponseFields())
			));
	}

	@Test
	void getTask() throws Exception {
		TaskCommentResponse comment = new TaskCommentResponse(
			5001L,
			1001L,
			new TaskCommentAuthorResponse(3001L, "Actor"),
			"진행 상황 확인했습니다.",
			false,
			true,
			true,
			1780917000L,
			1780917000L
		);
		TaskDetailResponse response = new TaskDetailResponse(
			1001L,
			2001L,
			"Login UI",
			"### Login screen\n- Add email field",
			TaskStatus.IN_PROGRESS,
			new TaskAssigneeResponse(3002L, 1002L, "Assignee", "assignee@example.com"),
			TaskPriority.HIGH,
			START_DATE,
			DUE_DATE,
			List.of("frontend", "ui"),
			35,
			1001L,
			1780916400L,
			1780920000L,
			ApiListData.of(List.of(comment), 2L)
		);

		when(taskService.get(any(CurrentUser.class), eq(2001L), eq(1001L))).thenReturn(response);
		authenticate();

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/tasks/{taskId}", 2001L, 1001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("workspaces-tasks-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				taskPathParameters("작업을 조회할 워크스페이스 식별자입니다.", "조회할 작업 식별자입니다."),
				authRequestHeaders(),
				responseHeaders(contentTypeResponseHeader()),
				responseFields(taskDetailResponseFields())
			));
	}

	@Test
	void updateTask() throws Exception {
		String requestBody = """
			{
			  "title": "Login UI refinement",
			  "descriptionMarkdown": "### Login screen\\n- Add validation",
			  "status": "DONE",
			  "assigneeMemberId": 3002,
			  "priority": "MEDIUM",
			  "startDate": 1780876800,
			  "dueDate": 1781222400,
			  "tags": ["frontend", "qa"]
			}
			""";
		authenticate();

		mockMvc.perform(patch("/v1/workspaces/{workspaceId}/tasks/{taskId}", 2001L, 1001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andDo(document("workspaces-tasks-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				taskPathParameters("전체 수정할 작업이 속한 워크스페이스 식별자입니다.", "전체 수정할 작업 식별자입니다."),
				authRequestHeaders(),
				requestFields(taskRequestFields()),
				responseHeaders(contentTypeResponseHeader()),
				responseFields(emptyResponseFields())
			));
	}

	@Test
	void updateTaskStatus() throws Exception {
		String requestBody = """
			{
			  "status": "IN_PROGRESS"
			}
			""";
		authenticate();

		mockMvc.perform(patch("/v1/workspaces/{workspaceId}/tasks/{taskId}/status", 2001L, 1001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andDo(document("workspaces-tasks-status-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				taskPathParameters("상태를 변경할 작업이 속한 워크스페이스 식별자입니다.", "상태를 변경할 작업 식별자입니다."),
				authRequestHeaders(),
				requestFields(
					fieldWithPath("status").type(JsonFieldType.STRING).description("변경할 작업 상태입니다. link:workspaces-tasks-enum-reference.html#task-status[TaskStatus]를 참고합니다.")
				),
				responseHeaders(contentTypeResponseHeader()),
				responseFields(emptyResponseFields())
			));
	}

	@Test
	void updateTaskProgress() throws Exception {
		String requestBody = """
			{
			  "progress": 65
			}
			""";
		authenticate();

		mockMvc.perform(patch("/v1/workspaces/{workspaceId}/tasks/{taskId}/progress", 2001L, 1001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andDo(document("workspaces-tasks-progress-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				taskPathParameters("진행도를 변경할 작업이 속한 워크스페이스 식별자입니다.", "진행도를 변경할 작업 식별자입니다."),
				authRequestHeaders(),
				requestFields(
					fieldWithPath("progress").type(JsonFieldType.NUMBER).description("작업 진행도입니다. 0부터 100까지의 정수입니다.")
				),
				responseHeaders(contentTypeResponseHeader()),
				responseFields(emptyResponseFields())
			));
	}

	@Test
	void taskHistories() throws Exception {
		TaskHistoryResponse history = new TaskHistoryResponse(
			5001L,
			1780920000L,
			new TaskHistoryActorResponse(3001L, 1001L, "Actor"),
			new TaskHistoryTargetResponse("TASK", 1001L, "Login UI"),
			TaskHistoryAction.STATUS_CHANGED,
			List.of(new TaskHistoryChangeResponse(TaskHistoryElement.STATUS, "TODO", "IN_PROGRESS"))
		);

		when(taskService.taskHistories(any(CurrentUser.class), eq(2001L), eq(1001L), eq(0), eq(20)))
			.thenReturn(ApiListData.of(List.of(history), 1L));
		authenticate();

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/tasks/{taskId}/histories", 2001L, 1001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.param("page", "0")
				.param("size", "20")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("workspaces-tasks-histories",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				taskPathParameters("작업 이력을 조회할 워크스페이스 식별자입니다.", "이력을 조회할 작업 식별자입니다."),
				authRequestHeaders(),
				queryParameters(
					numberParameter("page").description("0부터 시작하는 페이지 번호입니다. 생략 시 서버 기본값을 사용합니다.").optional(),
					numberParameter("size").description("페이지 크기입니다. 생략 시 서버 기본값을 사용합니다.").optional()
				),
				responseHeaders(contentTypeResponseHeader()),
				responseFields(taskHistoryListResponseFields())
			));
	}

	private void authenticate() {
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1001", "actor@example.com", "Actor"), List.of())
		);
	}

	private org.springframework.restdocs.snippet.Snippet workspacePathParameters(String workspaceDescription) {
		return pathParameters(numberParameter("workspaceId").description(workspaceDescription));
	}

	private org.springframework.restdocs.snippet.Snippet taskPathParameters(String workspaceDescription, String taskDescription) {
		return pathParameters(
			numberParameter("workspaceId").description(workspaceDescription),
			numberParameter("taskId").description(taskDescription)
		);
	}

	private org.springframework.restdocs.snippet.Snippet authRequestHeaders() {
		return requestHeaders(
			headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. `Bearer {token}` 형식으로 전달합니다."),
			headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional(),
			headerWithName(HttpHeaders.CONTENT_TYPE).description("요청 본문 미디어 타입입니다. 본문이 없는 GET 요청에서는 생략됩니다.").optional()
		);
	}

	private org.springframework.restdocs.headers.HeaderDescriptor contentTypeResponseHeader() {
		return headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.");
	}

	private org.springframework.restdocs.payload.FieldDescriptor[] taskRequestFields() {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath("title").type(JsonFieldType.STRING).description("작업 제목입니다. 공백만 전달할 수 없습니다."),
			fieldWithPath("descriptionMarkdown").type(JsonFieldType.STRING).description("작업 설명 Markdown 원문입니다. 서버는 원문 문자열을 저장하고 렌더링은 클라이언트가 수행합니다.").optional(),
			fieldWithPath("status").type(JsonFieldType.STRING).description("작업 상태입니다. 생성 시 생략하면 ``TODO``로 처리됩니다. link:workspaces-tasks-enum-reference.html#task-status[TaskStatus]를 참고합니다.").optional(),
			fieldWithPath("assigneeMemberId").type(JsonFieldType.NUMBER).description("작업 담당 워크스페이스 멤버 식별자입니다. ``null``이면 미할당 상태입니다.").optional(),
			fieldWithPath("priority").type(JsonFieldType.STRING).description("작업 우선순위입니다. link:workspaces-tasks-enum-reference.html#task-priority[TaskPriority]를 참고합니다."),
			fieldWithPath("startDate").type(JsonFieldType.NUMBER).description("작업 시작 예정 시각입니다. Unix epoch seconds 기준입니다.").optional(),
			fieldWithPath("dueDate").type(JsonFieldType.NUMBER).description("작업 마감 예정 시각입니다. Unix epoch seconds 기준입니다.").optional(),
			fieldWithPath("tags").type(JsonFieldType.ARRAY).description("작업 태그 목록입니다. 최대 10개까지 허용하며 동일 작업 안에서 같은 태그는 정규화 이름 기준으로 중복 제거됩니다.").attributes(stringArrayElements()).optional()
		};
	}

	private org.springframework.restdocs.payload.FieldDescriptor[] taskCreateRequestFields() {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath("title").type(JsonFieldType.STRING).description("작업 제목입니다. 공백만 전달할 수 없습니다."),
			fieldWithPath("descriptionMarkdown").type(JsonFieldType.STRING).description("작업 설명 Markdown 원문입니다. 서버는 원문 문자열을 저장하고 렌더링은 클라이언트가 수행합니다.").optional(),
			fieldWithPath("status").type(JsonFieldType.STRING).description("작업 상태입니다. 생성 시 생략하면 ``TODO``로 처리됩니다. link:workspaces-tasks-enum-reference.html#task-status[TaskStatus]를 참고합니다.").optional(),
			fieldWithPath("assigneeMemberId").type(JsonFieldType.NUMBER).description("작업 담당 워크스페이스 멤버 식별자입니다. ``null``이면 미할당 상태입니다.").optional(),
			fieldWithPath("priority").type(JsonFieldType.STRING).description("작업 우선순위입니다. link:workspaces-tasks-enum-reference.html#task-priority[TaskPriority]를 참고합니다."),
			fieldWithPath("startDate").type(JsonFieldType.NUMBER).description("작업 시작 예정 시각입니다. Unix epoch seconds 기준입니다.").optional(),
			fieldWithPath("dueDate").type(JsonFieldType.NUMBER).description("작업 마감 예정 시각입니다. Unix epoch seconds 기준입니다.").optional(),
			fieldWithPath("progress").type(JsonFieldType.NUMBER).description("작업 생성 시 적용할 진행도입니다. 생략하면 0으로 처리되며, 0부터 100까지의 정수입니다.").optional(),
			fieldWithPath("tags").type(JsonFieldType.ARRAY).description("작업 태그 목록입니다. 최대 10개까지 허용하며 동일 작업 안에서 같은 태그는 정규화 이름 기준으로 중복 제거됩니다.").attributes(stringArrayElements()).optional()
		};
	}

	private org.springframework.restdocs.payload.FieldDescriptor[] createdResponseFields(String createdIdDescription) {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성 결과 데이터입니다."),
			fieldWithPath("data.createdId").type(JsonFieldType.NUMBER).description(createdIdDescription),
			fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
		};
	}

	private org.springframework.restdocs.payload.FieldDescriptor[] emptyResponseFields() {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("빈 객체입니다."),
			fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
		};
	}

	private org.springframework.restdocs.payload.FieldDescriptor[] taskListResponseFields() {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("작업 목록 데이터입니다."),
			fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("작업 목록입니다."),
			fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("작업 식별자입니다."),
			fieldWithPath("data.items[].workspaceId").type(JsonFieldType.NUMBER).description("작업이 속한 워크스페이스 식별자입니다."),
			fieldWithPath("data.items[].title").type(JsonFieldType.STRING).description("작업 제목입니다."),
			fieldWithPath("data.items[].status").type(JsonFieldType.STRING).description("작업 상태입니다. link:workspaces-tasks-enum-reference.html#task-status[TaskStatus]를 참고합니다."),
			fieldWithPath("data.items[].assignee").type(JsonFieldType.OBJECT).description("작업 담당자 정보입니다. 미할당 작업에서는 ``null``일 수 있습니다.").optional(),
			fieldWithPath("data.items[].assignee.memberId").type(JsonFieldType.NUMBER).description("담당 워크스페이스 멤버 식별자입니다.").optional(),
			fieldWithPath("data.items[].assignee.userId").type(JsonFieldType.NUMBER).description("담당 사용자 식별자입니다.").optional(),
			fieldWithPath("data.items[].assignee.name").type(JsonFieldType.STRING).description("담당자 표시 이름입니다.").optional(),
			fieldWithPath("data.items[].assignee.email").type(JsonFieldType.STRING).description("담당자 이메일입니다.").optional(),
			fieldWithPath("data.items[].priority").type(JsonFieldType.STRING).description("작업 우선순위입니다. link:workspaces-tasks-enum-reference.html#task-priority[TaskPriority]를 참고합니다."),
			fieldWithPath("data.items[].startDate").type(JsonFieldType.NUMBER).description("작업 시작 예정 시각입니다. Unix epoch seconds 기준입니다.").optional(),
			fieldWithPath("data.items[].dueDate").type(JsonFieldType.NUMBER).description("작업 마감 예정 시각입니다. Unix epoch seconds 기준입니다.").optional(),
			fieldWithPath("data.items[].tags").type(JsonFieldType.ARRAY).description("작업 태그 목록입니다."),
			fieldWithPath("data.items[].progress").type(JsonFieldType.NUMBER).description("작업 진행도입니다. 0부터 100까지의 정수입니다."),
			fieldWithPath("data.items[].createdAt").type(JsonFieldType.NUMBER).description("작업 생성 시각입니다. Unix epoch seconds 기준입니다."),
			fieldWithPath("data.items[].updatedAt").type(JsonFieldType.NUMBER).description("작업 최종 수정 시각입니다. Unix epoch seconds 기준입니다."),
			fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER).description("검색 조건에 맞는 전체 작업 수입니다."),
			fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
		};
	}

	private org.springframework.restdocs.payload.FieldDescriptor[] taskDetailResponseFields() {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("작업 상세 데이터입니다."),
			fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("작업 식별자입니다."),
			fieldWithPath("data.workspaceId").type(JsonFieldType.NUMBER).description("작업이 속한 워크스페이스 식별자입니다."),
			fieldWithPath("data.title").type(JsonFieldType.STRING).description("작업 제목입니다."),
			fieldWithPath("data.descriptionMarkdown").type(JsonFieldType.STRING).description("작업 설명 Markdown 원문입니다.").optional(),
			fieldWithPath("data.status").type(JsonFieldType.STRING).description("작업 상태입니다. link:workspaces-tasks-enum-reference.html#task-status[TaskStatus]를 참고합니다."),
			fieldWithPath("data.assignee").type(JsonFieldType.OBJECT).description("작업 담당자 정보입니다. 미할당 작업에서는 ``null``일 수 있습니다.").optional(),
			fieldWithPath("data.assignee.memberId").type(JsonFieldType.NUMBER).description("담당 워크스페이스 멤버 식별자입니다.").optional(),
			fieldWithPath("data.assignee.userId").type(JsonFieldType.NUMBER).description("담당 사용자 식별자입니다.").optional(),
			fieldWithPath("data.assignee.name").type(JsonFieldType.STRING).description("담당자 표시 이름입니다.").optional(),
			fieldWithPath("data.assignee.email").type(JsonFieldType.STRING).description("담당자 이메일입니다.").optional(),
			fieldWithPath("data.priority").type(JsonFieldType.STRING).description("작업 우선순위입니다. link:workspaces-tasks-enum-reference.html#task-priority[TaskPriority]를 참고합니다."),
			fieldWithPath("data.startDate").type(JsonFieldType.NUMBER).description("작업 시작 예정 시각입니다. Unix epoch seconds 기준입니다.").optional(),
			fieldWithPath("data.dueDate").type(JsonFieldType.NUMBER).description("작업 마감 예정 시각입니다. Unix epoch seconds 기준입니다.").optional(),
			fieldWithPath("data.tags").type(JsonFieldType.ARRAY).description("작업 태그 목록입니다."),
			fieldWithPath("data.progress").type(JsonFieldType.NUMBER).description("작업 진행도입니다. 0부터 100까지의 정수입니다."),
			fieldWithPath("data.createdByUserId").type(JsonFieldType.NUMBER).description("작업 생성 사용자 식별자입니다."),
			fieldWithPath("data.createdAt").type(JsonFieldType.NUMBER).description("작업 생성 시각입니다. Unix epoch seconds 기준입니다."),
			fieldWithPath("data.updatedAt").type(JsonFieldType.NUMBER).description("작업 최종 수정 시각입니다. Unix epoch seconds 기준입니다."),
			fieldWithPath("data.commentPage").type(JsonFieldType.OBJECT).description("작업 상세 초기 로드에 포함되는 댓글 첫 페이지입니다."),
			fieldWithPath("data.commentPage.items").type(JsonFieldType.ARRAY).description("생성 시각이 오래된 순서로 정렬된 작업 댓글 첫 페이지 목록입니다. 최대 20개까지 포함됩니다."),
			fieldWithPath("data.commentPage.items[].id").type(JsonFieldType.NUMBER).description("작업 댓글 식별자입니다."),
			fieldWithPath("data.commentPage.items[].taskId").type(JsonFieldType.NUMBER).description("댓글이 속한 작업 식별자입니다."),
			fieldWithPath("data.commentPage.items[].author").type(JsonFieldType.OBJECT).description("댓글 작성자 정보입니다."),
			fieldWithPath("data.commentPage.items[].author.memberId").type(JsonFieldType.NUMBER).description("댓글 작성자의 워크스페이스 멤버 식별자입니다."),
			fieldWithPath("data.commentPage.items[].author.displayName").type(JsonFieldType.STRING).description("댓글 작성 시점의 작성자 표시 이름 스냅샷입니다."),
			fieldWithPath("data.commentPage.items[].contentMarkdown").type(JsonFieldType.STRING).description("작업 댓글 Markdown 원문입니다."),
			fieldWithPath("data.commentPage.items[].edited").type(JsonFieldType.BOOLEAN).description("생성 이후 댓글 내용 수정이 발생했는지 여부입니다."),
			fieldWithPath("data.commentPage.items[].editable").type(JsonFieldType.BOOLEAN).description("요청자가 이 댓글을 수정하거나 삭제할 수 있는지 여부입니다."),
			fieldWithPath("data.commentPage.items[].ownedByRequester").type(JsonFieldType.BOOLEAN).description("이 댓글이 현재 요청자가 작성한 댓글인지 여부입니다."),
			fieldWithPath("data.commentPage.items[].createdAt").type(JsonFieldType.NUMBER).description("작업 댓글 생성 시각입니다. Unix epoch seconds 기준입니다."),
			fieldWithPath("data.commentPage.items[].updatedAt").type(JsonFieldType.NUMBER).description("작업 댓글 최종 수정 시각입니다. Unix epoch seconds 기준입니다."),
			fieldWithPath("data.commentPage.totalCount").type(JsonFieldType.NUMBER).description("작업에 달린 전체 활성 댓글 수입니다."),
			fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
		};
	}

	private org.springframework.restdocs.payload.FieldDescriptor[] taskHistoryListResponseFields() {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("작업 변경 이력 목록 데이터입니다."),
			fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("작업 변경 이력 목록입니다."),
			fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("작업 변경 이력 식별자입니다."),
			fieldWithPath("data.items[].occurredAt").type(JsonFieldType.NUMBER).description("변경 발생 시각입니다. Unix epoch seconds 기준입니다."),
			fieldWithPath("data.items[].actor").type(JsonFieldType.OBJECT).description("변경 수행자 정보입니다."),
			fieldWithPath("data.items[].actor.memberId").type(JsonFieldType.NUMBER).description("변경 수행자의 워크스페이스 멤버 식별자입니다. 시스템 이벤트에서는 ``null``일 수 있습니다.").optional(),
			fieldWithPath("data.items[].actor.userId").type(JsonFieldType.NUMBER).description("변경 수행자의 사용자 식별자입니다. 시스템 이벤트에서는 ``null``일 수 있습니다.").optional(),
			fieldWithPath("data.items[].actor.displayName").type(JsonFieldType.STRING).description("변경 당시 수행자 표시 이름 스냅샷입니다."),
			fieldWithPath("data.items[].target").type(JsonFieldType.OBJECT).description("변경 대상 정보입니다."),
			fieldWithPath("data.items[].target.type").type(JsonFieldType.STRING).description("대상 타입입니다. 작업 이력에서는 ``TASK``입니다."),
			fieldWithPath("data.items[].target.taskId").type(JsonFieldType.NUMBER).description("변경 대상 작업 식별자입니다."),
			fieldWithPath("data.items[].target.displayName").type(JsonFieldType.STRING).description("변경 당시 작업 제목 스냅샷입니다."),
			fieldWithPath("data.items[].action").type(JsonFieldType.STRING).description("변경 액션입니다. link:workspaces-tasks-enum-reference.html#task-history-action[TaskHistoryAction]을 참고합니다."),
			fieldWithPath("data.items[].changes").type(JsonFieldType.ARRAY).description("문장 조합 전 의미 단위 변경 목록입니다."),
			fieldWithPath("data.items[].changes[].element").type(JsonFieldType.STRING).description("변경 요소입니다. link:workspaces-tasks-enum-reference.html#task-history-element[TaskHistoryElement]를 참고합니다."),
			fieldWithPath("data.items[].changes[].from").type(JsonFieldType.VARIES).description("변경 전 값입니다. 값 타입은 ``element``에 따라 달라집니다.").optional(),
			fieldWithPath("data.items[].changes[].to").type(JsonFieldType.VARIES).description("변경 후 값입니다. 값 타입은 ``element``에 따라 달라집니다.").optional(),
			fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER).description("검색 조건에 맞는 전체 이력 수입니다."),
			fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
		};
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
