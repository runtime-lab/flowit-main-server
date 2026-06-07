package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.activity.service.WorkspaceActivityService;
import dev.runtime_lab.flowit.domain.workspace.controller.WorkspaceController;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceUpdateRequest;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
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
class WorkspaceUpdateApiDocsTest {

	private final WorkspaceService workspaceService = mock(WorkspaceService.class);
	private final WorkspaceActivityService workspaceActivityService = mock(WorkspaceActivityService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new WorkspaceController(workspaceService, workspaceActivityService))
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
	void updateWorkspace() throws Exception {
		String requestBody = """
			{
			  "name": "Flowit Renamed",
			  "description": "Updated workspace"
			}
			""";
		WorkspaceResponse response = new WorkspaceResponse(
			2001L,
			"Flowit Renamed",
			"Updated workspace",
			"A1B2-C3D4-E5F6",
			1779889000L,
			1779889100L
		);

		when(workspaceService.update(any(CurrentUser.class), eq(2001L), any(WorkspaceUpdateRequest.class)))
			.thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "admin@example.com", "Admin"), List.of())
		);

		mockMvc.perform(patch("/v1/workspaces/{workspaceId}", 2001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andDo(document("workspaces-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(
					parameterWithName("workspaceId").description("수정할 워크스페이스 식별자입니다.")
				),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. `Bearer {token}` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.CONTENT_TYPE).description("요청 본문 미디어 타입입니다. `application/json`을 사용합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				requestFields(
					fieldWithPath("name").type(JsonFieldType.STRING).description("워크스페이스 이름입니다. 생략하면 기존 이름을 유지합니다. 빈 값은 거부됩니다.").optional(),
					fieldWithPath("description").type(JsonFieldType.STRING).description("워크스페이스 설명입니다. 생략하면 기존 설명을 유지합니다. 빈 값은 설명 제거로 처리합니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("수정된 워크스페이스 응답 데이터입니다."),
					fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("워크스페이스 식별자입니다."),
					fieldWithPath("data.name").type(JsonFieldType.STRING).description("워크스페이스 이름입니다."),
					fieldWithPath("data.description").type(JsonFieldType.STRING).description("워크스페이스 설명입니다.").optional(),
					fieldWithPath("data.inviteCode").type(JsonFieldType.STRING).description("워크스페이스 초대 코드입니다."),
					fieldWithPath("data.createdAt").type(JsonFieldType.NUMBER).description("워크스페이스 생성 시각입니다. Unix epoch seconds 기준입니다."),
					fieldWithPath("data.updatedAt").type(JsonFieldType.NUMBER).description("워크스페이스 수정 시각입니다. Unix epoch seconds 기준입니다."),
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
