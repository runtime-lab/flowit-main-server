package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.workspace.controller.WorkspaceJoinRequestController;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinByInviteCodeRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestHistoryResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestResultResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestsResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestMethod;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
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
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class WorkspaceJoinRequestsApiDocsTest {

	private final WorkspaceJoinRequestService workspaceJoinRequestService = mock(WorkspaceJoinRequestService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new WorkspaceJoinRequestController(workspaceJoinRequestService))
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
	void joinByInviteCode() throws Exception {
		WorkspaceJoinRequestResultResponse response = joinedResultResponse();

		when(workspaceJoinRequestService.joinByInviteCode(
			any(CurrentUser.class),
			any(WorkspaceJoinByInviteCodeRequest.class)
		)).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "member@example.com", "Member"), List.of())
		);

		mockMvc.perform(post("/v1/workspaces/join-requests/invite-code")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "inviteCode": "A1B2-C3D4-E5F6"
					}
					"""))
			.andExpect(status().isCreated())
			.andDo(document("workspaces-join-requests-invite-code",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. ``Bearer {token}`` 형식입니다."),
					headerWithName(HttpHeaders.CONTENT_TYPE).description("요청 본문 media type입니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 media type입니다.").optional()
				),
				requestFields(
					fieldWithPath("inviteCode").type(JsonFieldType.STRING)
						.description("가입 요청을 생성할 때 사용하는 워크스페이스 초대 코드입니다.")
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 media type입니다."),
					headerWithName(HttpHeaders.LOCATION).description("생성된 가입 요청 리소스의 위치입니다.")
				),
				workspaceJoinRequestResultResponseFields("data.")
			));
	}

	@Test
	void joinRequests() throws Exception {
		WorkspaceJoinRequestsResponse response = new WorkspaceJoinRequestsResponse(List.of(joinedResponse()));

		when(workspaceJoinRequestService.requests(any(CurrentUser.class), eq(2001L))).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1001", "owner@example.com", "Owner"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/join-requests", 2001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("workspaces-join-requests",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(
					parameterWithName("workspaceId").description("가입 요청 이력을 조회할 워크스페이스 ID입니다.")
				),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. ``Bearer {token}`` 형식입니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 media type입니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 media type입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("워크스페이스 가입 요청 목록 응답입니다."),
					fieldWithPath("data.joinRequests").type(JsonFieldType.ARRAY).description("워크스페이스 가입 요청 목록입니다."),
					fieldWithPath("data.joinRequests[].joinRequestId").type(JsonFieldType.NUMBER)
						.description("워크스페이스 가입 요청 ID입니다."),
					fieldWithPath("data.joinRequests[].workspaceId").type(JsonFieldType.NUMBER)
						.description("대상 워크스페이스 ID입니다."),
					fieldWithPath("data.joinRequests[].workspaceName").type(JsonFieldType.STRING)
						.description("대상 워크스페이스 이름입니다."),
					fieldWithPath("data.joinRequests[].userId").type(JsonFieldType.NUMBER)
						.description("가입 요청자 사용자 ID입니다."),
					fieldWithPath("data.joinRequests[].userName").type(JsonFieldType.STRING)
						.description("가입 요청자 이름입니다."),
					fieldWithPath("data.joinRequests[].userEmail").type(JsonFieldType.STRING)
						.description("가입 요청자 이메일입니다."),
					fieldWithPath("data.joinRequests[].memberId").type(JsonFieldType.NUMBER)
						.description("생성된 워크스페이스 멤버 ID입니다. ``JOINED`` 이전에는 ``null`` 입니다.").optional(),
					fieldWithPath("data.joinRequests[].method").type(JsonFieldType.STRING)
						.description("가입 요청 생성 수단입니다. link:enum-reference.html#workspace-join-request-method[WorkspaceJoinRequestMethod]를 참고합니다."),
					fieldWithPath("data.joinRequests[].inviteCode").type(JsonFieldType.STRING)
						.description("요청 시점에 사용된 초대 코드 스냅샷입니다."),
					fieldWithPath("data.joinRequests[].status").type(JsonFieldType.STRING)
						.description("현재 가입 요청 상태입니다. link:enum-reference.html#workspace-join-request-status[WorkspaceJoinRequestStatus]를 참고합니다."),
					fieldWithPath("data.joinRequests[].requestedAt").type(JsonFieldType.NUMBER)
						.description("가입 요청 생성 시각입니다. Unix epoch seconds 값입니다."),
					fieldWithPath("data.joinRequests[].readyAt").type(JsonFieldType.NUMBER)
						.description("READY 상태 전이 시각입니다. Unix epoch seconds 값입니다.").optional(),
					fieldWithPath("data.joinRequests[].approvedAt").type(JsonFieldType.NUMBER)
						.description("승인 시각입니다. Unix epoch seconds 값입니다.").optional(),
					fieldWithPath("data.joinRequests[].joinedAt").type(JsonFieldType.NUMBER)
						.description("가입 완료 시각입니다. Unix epoch seconds 값입니다.").optional(),
					fieldWithPath("data.joinRequests[].failedAt").type(JsonFieldType.NUMBER)
						.description("실패 상태 전이 시각입니다. Unix epoch seconds 값입니다.").optional(),
					fieldWithPath("data.joinRequests[].failureCode").type(JsonFieldType.STRING)
						.description("실패 분류 코드입니다.").optional(),
					fieldWithPath("data.joinRequests[].failureMessage").type(JsonFieldType.STRING)
						.description("실패 상세 메시지입니다.").optional(),
					fieldWithPath("data.joinRequests[].histories").type(JsonFieldType.ARRAY)
						.description("상태 전이 이력 목록입니다."),
					fieldWithPath("data.joinRequests[].histories[].historyId").type(JsonFieldType.NUMBER)
						.description("상태 전이 이력 ID입니다."),
					fieldWithPath("data.joinRequests[].histories[].fromStatus").type(JsonFieldType.STRING)
						.description("이전 상태입니다. 최초 요청 이력에서는 ``null``입니다.").optional(),
					fieldWithPath("data.joinRequests[].histories[].toStatus").type(JsonFieldType.STRING)
						.description("다음 상태입니다."),
					fieldWithPath("data.joinRequests[].histories[].changedByUserId").type(JsonFieldType.NUMBER)
						.description("상태 전이를 발생시킨 사용자 ID입니다.").optional(),
					fieldWithPath("data.joinRequests[].histories[].changedAt").type(JsonFieldType.NUMBER)
						.description("상태 전이 시각입니다. Unix epoch seconds 값입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 확장 데이터입니다.")
				)
			));
	}

	private org.springframework.restdocs.payload.ResponseFieldsSnippet workspaceJoinRequestResultResponseFields(String prefix) {
		return responseFields(
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부입니다."),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 워크스페이스 가입 요청 응답입니다."),
			fieldWithPath(prefix + "joinRequestId").type(JsonFieldType.NUMBER).description("워크스페이스 가입 요청 ID입니다."),
			fieldWithPath(prefix + "workspaceId").type(JsonFieldType.NUMBER).description("대상 워크스페이스 ID입니다."),
			fieldWithPath(prefix + "workspaceName").type(JsonFieldType.STRING).description("대상 워크스페이스 이름입니다."),
			fieldWithPath(prefix + "userId").type(JsonFieldType.NUMBER).description("가입 요청자 사용자 ID입니다."),
			fieldWithPath(prefix + "userName").type(JsonFieldType.STRING).description("가입 요청자 이름입니다."),
			fieldWithPath(prefix + "userEmail").type(JsonFieldType.STRING).description("가입 요청자 이메일입니다."),
			fieldWithPath(prefix + "memberId").type(JsonFieldType.NUMBER)
				.description("생성된 워크스페이스 멤버 ID입니다."),
			fieldWithPath(prefix + "method").type(JsonFieldType.STRING)
				.description("가입 요청 생성 수단입니다. link:enum-reference.html#workspace-join-request-method[WorkspaceJoinRequestMethod]를 참고합니다."),
			fieldWithPath(prefix + "inviteCode").type(JsonFieldType.STRING)
				.description("요청 시점에 사용된 초대 코드 스냅샷입니다."),
			fieldWithPath(prefix + "status").type(JsonFieldType.STRING)
				.description("현재 가입 요청 상태입니다. link:enum-reference.html#workspace-join-request-status[WorkspaceJoinRequestStatus]를 참고합니다."),
			fieldWithPath(prefix + "joinedAt").type(JsonFieldType.NUMBER)
				.description("가입 완료 시각입니다. Unix epoch seconds 값입니다."),
			fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 확장 데이터입니다.")
		);
	}

	private WorkspaceJoinRequestResultResponse joinedResultResponse() {
		return new WorkspaceJoinRequestResultResponse(
			3001L,
			2001L,
			"Flowit",
			1003L,
			"Member",
			"member@example.com",
			4001L,
			WorkspaceJoinRequestMethod.INVITE_CODE,
			"A1B2-C3D4-E5F6",
			WorkspaceJoinRequestStatus.JOINED,
			1779888930L
		);
	}

	private WorkspaceJoinRequestResponse joinedResponse() {
		return new WorkspaceJoinRequestResponse(
			3001L,
			2001L,
			"Flowit",
			1003L,
			"Member",
			"member@example.com",
			4001L,
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
					5001L,
					null,
					WorkspaceJoinRequestStatus.PENDING,
					1003L,
					1779888900L
				),
				new WorkspaceJoinRequestHistoryResponse(
					5002L,
					WorkspaceJoinRequestStatus.PENDING,
					WorkspaceJoinRequestStatus.READY,
					1003L,
					1779888910L
				),
				new WorkspaceJoinRequestHistoryResponse(
					5003L,
					WorkspaceJoinRequestStatus.READY,
					WorkspaceJoinRequestStatus.APPROVED,
					1003L,
					1779888920L
				),
				new WorkspaceJoinRequestHistoryResponse(
					5004L,
					WorkspaceJoinRequestStatus.APPROVED,
					WorkspaceJoinRequestStatus.JOINED,
					1003L,
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
