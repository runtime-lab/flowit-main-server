package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.workspace.controller.WorkspaceMemberController;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMembersResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class WorkspaceMembersApiDocsTest {

	private final WorkspaceMemberService workspaceMemberService = mock(WorkspaceMemberService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new WorkspaceMemberController(workspaceMemberService))
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
	void workspaceMembers() throws Exception {
		WorkspaceMembersResponse response = new WorkspaceMembersResponse(
			"A1B2-C3D4-E5F6",
			List.of(
				new WorkspaceMemberResponse(
					3001L,
					"Owner",
					"owner@example.com",
					UserStatus.ACTIVE,
					WorkspaceMemberRole.OWNER
				),
				new WorkspaceMemberResponse(
					3002L,
					"Admin",
					"admin@example.com",
					UserStatus.ACTIVE,
					WorkspaceMemberRole.ADMIN
				),
				new WorkspaceMemberResponse(
					3003L,
					"Member",
					"member@example.com",
					UserStatus.ACTIVE,
					WorkspaceMemberRole.MEMBER
				)
			)
		);

		when(workspaceMemberService.members(any(CurrentUser.class), eq(2001L))).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "owner@example.com", "Owner"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/members", 2001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("workspaces-members",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(
					parameterWithName("workspaceId").description("멤버 목록을 조회할 워크스페이스 식별자입니다.")
				),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. `Bearer {token}` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("워크스페이스 멤버 목록 응답 데이터입니다."),
					fieldWithPath("data.inviteCode").type(JsonFieldType.STRING).description("워크스페이스 초대 코드입니다."),
					fieldWithPath("data.members").type(JsonFieldType.ARRAY).description("워크스페이스 활성 멤버 목록입니다."),
					fieldWithPath("data.members[].memberId").type(JsonFieldType.NUMBER).description("워크스페이스 멤버 식별자입니다. 강퇴, 역할 변경 등 멤버십 대상 액션에 사용할 수 있습니다."),
					fieldWithPath("data.members[].name").type(JsonFieldType.STRING).description("멤버 이름입니다."),
					fieldWithPath("data.members[].email").type(JsonFieldType.STRING).description("멤버 이메일입니다."),
					fieldWithPath("data.members[].status").type(JsonFieldType.STRING).description("멤버 사용자 상태입니다. link:enum-reference.html#user-status[UserStatus]를 참고합니다."),
					fieldWithPath("data.members[].role").type(JsonFieldType.STRING).description("워크스페이스 멤버 역할입니다. link:enum-reference.html#workspace-member-role[WorkspaceMemberRole]을 참고합니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
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
