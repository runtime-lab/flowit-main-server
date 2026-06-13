package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.task.controller.TaskCommentController;
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

import static dev.runtime_lab.flowit.docs.support.DocumentedTypes.numberParameter;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
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
class TaskCommentApiDocsTest {

	private final TaskCommentService taskCommentService = mock(TaskCommentService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new TaskCommentController(taskCommentService))
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
	void createTaskComment() throws Exception {
		String requestBody = """
			{
			  "contentMarkdown": "확인했습니다. 이 방향으로 진행하겠습니다."
			}
			""";

		when(taskCommentService.create(any(CurrentUser.class), eq(2001L), eq(1001L), any(TaskCommentCreateRequest.class)))
			.thenReturn(new TaskCommentCreateResponse(5001L, 1780916400L));
		authenticate();

		mockMvc.perform(post("/v1/workspaces/{workspaceId}/tasks/{taskId}/comments", 2001L, 1001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andDo(document("workspaces-tasks-comments-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				taskPathParameters("댓글을 생성할 워크스페이스 식별자입니다.", "댓글을 생성할 작업 식별자입니다."),
				authRequestHeaders(),
				requestFields(commentRequestFields()),
				responseHeaders(
					headerWithName(HttpHeaders.LOCATION).description("생성된 작업 댓글 리소스의 위치입니다."),
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.")
				),
				responseFields(createdResponseFields("생성된 작업 댓글 식별자입니다."))
			));
	}

	@Test
	void listTaskComments() throws Exception {
		TaskCommentResponse comment = new TaskCommentResponse(
			5001L,
			1001L,
			new TaskCommentAuthorResponse(3001L, "Actor"),
			"수정된 댓글입니다.",
			true,
			true,
			true,
			1780916400L,
			1780920000L
		);

		when(taskCommentService.comments(any(CurrentUser.class), eq(2001L), eq(1001L), eq(0), eq(20)))
			.thenReturn(ApiListData.of(List.of(comment), 1L));
		authenticate();

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/tasks/{taskId}/comments", 2001L, 1001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.param("page", "0")
				.param("size", "20")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("workspaces-tasks-comments-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				taskPathParameters("댓글을 조회할 워크스페이스 식별자입니다.", "댓글을 조회할 작업 식별자입니다."),
				authRequestHeaders(),
				queryParameters(
					numberParameter("page").description("0부터 시작하는 페이지 번호입니다. 생략 시 서버 기본값을 사용합니다.").optional(),
					numberParameter("size").description("페이지 크기입니다. 생략 시 20개를 반환합니다.").optional()
				),
				responseHeaders(contentTypeResponseHeader()),
				responseFields(commentListResponseFields())
			));
	}

	@Test
	void updateTaskComment() throws Exception {
		String requestBody = """
			{
			  "contentMarkdown": "검토 후 내용을 수정했습니다."
			}
			""";
		authenticate();

		mockMvc.perform(patch("/v1/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}", 2001L, 1001L, 5001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andDo(document("workspaces-tasks-comments-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				commentPathParameters(
					"댓글을 수정할 워크스페이스 식별자입니다.",
					"댓글이 속한 작업 식별자입니다.",
					"수정할 작업 댓글 식별자입니다."
				),
				authRequestHeaders(),
				requestFields(commentRequestFields()),
				responseHeaders(contentTypeResponseHeader()),
				responseFields(emptyResponseFields())
			));
	}

	@Test
	void deleteTaskComment() throws Exception {
		authenticate();

		mockMvc.perform(delete("/v1/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}", 2001L, 1001L, 5001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("workspaces-tasks-comments-delete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				commentPathParameters(
					"댓글을 삭제할 워크스페이스 식별자입니다.",
					"댓글이 속한 작업 식별자입니다.",
					"삭제할 작업 댓글 식별자입니다."
				),
				authRequestHeaders(),
				responseHeaders(contentTypeResponseHeader()),
				responseFields(emptyResponseFields())
			));
	}

	private void authenticate() {
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1001", "actor@example.com", "Actor"), List.of())
		);
	}

	private org.springframework.restdocs.snippet.Snippet taskPathParameters(
		String workspaceDescription,
		String taskDescription
	) {
		return pathParameters(
			numberParameter("workspaceId").description(workspaceDescription),
			numberParameter("taskId").description(taskDescription)
		);
	}

	private org.springframework.restdocs.snippet.Snippet commentPathParameters(
		String workspaceDescription,
		String taskDescription,
		String commentDescription
	) {
		return pathParameters(
			numberParameter("workspaceId").description(workspaceDescription),
			numberParameter("taskId").description(taskDescription),
			numberParameter("commentId").description(commentDescription)
		);
	}

	private org.springframework.restdocs.snippet.Snippet authRequestHeaders() {
		return requestHeaders(
			headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. `Bearer {token}` 형식으로 전달합니다."),
			headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional(),
			headerWithName(HttpHeaders.CONTENT_TYPE).description("요청 본문 미디어 타입입니다. 본문이 없는 요청에서는 생략합니다.").optional()
		);
	}

	private org.springframework.restdocs.headers.HeaderDescriptor contentTypeResponseHeader() {
		return headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.");
	}

	private org.springframework.restdocs.payload.FieldDescriptor[] commentRequestFields() {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath("contentMarkdown").type(JsonFieldType.STRING).description("작업 댓글 Markdown 원문입니다.")
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

	private org.springframework.restdocs.payload.FieldDescriptor[] commentListResponseFields() {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("작업 댓글 목록 데이터입니다."),
			fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("작업 댓글 목록입니다. 생성 시각이 오래된 순서로 정렬됩니다."),
			fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("작업 댓글 식별자입니다."),
			fieldWithPath("data.items[].taskId").type(JsonFieldType.NUMBER).description("댓글이 속한 작업 식별자입니다."),
			fieldWithPath("data.items[].author").type(JsonFieldType.OBJECT).description("댓글 작성자 정보입니다."),
			fieldWithPath("data.items[].author.memberId").type(JsonFieldType.NUMBER).description("댓글 작성자의 워크스페이스 멤버 식별자입니다."),
			fieldWithPath("data.items[].author.displayName").type(JsonFieldType.STRING).description("댓글 작성 시점의 작성자 표시 이름 스냅샷입니다."),
			fieldWithPath("data.items[].contentMarkdown").type(JsonFieldType.STRING).description("작업 댓글 Markdown 원문입니다."),
			fieldWithPath("data.items[].edited").type(JsonFieldType.BOOLEAN).description("생성 이후 댓글 내용 수정이 발생했는지 여부입니다."),
			fieldWithPath("data.items[].editable").type(JsonFieldType.BOOLEAN).description("요청자가 이 댓글을 수정하거나 삭제할 수 있는지 여부입니다."),
			fieldWithPath("data.items[].ownedByRequester").type(JsonFieldType.BOOLEAN).description("이 댓글이 현재 요청자가 작성한 댓글인지 여부입니다."),
			fieldWithPath("data.items[].createdAt").type(JsonFieldType.NUMBER).description("작업 댓글 생성 시각입니다. Unix epoch seconds 기준입니다."),
			fieldWithPath("data.items[].updatedAt").type(JsonFieldType.NUMBER).description("작업 댓글 최종 수정 시각입니다. Unix epoch seconds 기준입니다."),
			fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER).description("조건에 맞는 전체 작업 댓글 수입니다."),
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
