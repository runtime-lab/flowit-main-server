package dev.runtime_lab.flowit.docs;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import dev.runtime_lab.flowit.domain.activity.service.WorkspaceActivityService;
import dev.runtime_lab.flowit.domain.workspace.controller.WorkspaceController;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceResponse;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUserArgumentResolver;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static com.epages.restdocs.apispec.SimpleType.NUMBER;
import static dev.runtime_lab.flowit.docs.support.DocumentedTypes.numberParameter;
import static dev.runtime_lab.flowit.docs.support.ResponseFieldStability.experimental;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class WorkspaceGetApiDocsTest {

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
	void getWorkspace() throws Exception {
		WorkspaceResponse response = new WorkspaceResponse(
			2001L,
			"Flowit Team",
			"Product planning workspace",
			"A1B2-C3D4-E5F6",
			1779889000L,
			1779889100L
		);

		when(workspaceService.get(any(CurrentUser.class), eq(2001L))).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "user@example.com", "nickname"), List.of())
		);

		ParameterDescriptor workspaceIdParameter = numberParameter("workspaceId")
			.description("조회할 워크스페이스 식별자입니다.");
		HeaderDescriptor authorizationHeader = headerWithName(HttpHeaders.AUTHORIZATION)
			.description("JWT access token입니다. ``Bearer {token}`` 형식으로 전달합니다.");
		HeaderDescriptor acceptHeader = headerWithName(HttpHeaders.ACCEPT)
			.description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional();
		HeaderDescriptor responseContentTypeHeader = headerWithName(HttpHeaders.CONTENT_TYPE)
			.description("응답 본문 미디어 타입입니다.");
		FieldDescriptor[] responseFieldDescriptors = new FieldDescriptor[] {
			fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("워크스페이스 조회 응답 데이터입니다."),
			fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("워크스페이스 식별자입니다."),
			fieldWithPath("data.name").type(JsonFieldType.STRING).description("워크스페이스 이름입니다."),
			fieldWithPath("data.description").type(JsonFieldType.STRING).description("워크스페이스 설명입니다.").optional(),
			fieldWithPath("data.inviteCode").type(JsonFieldType.STRING).description("워크스페이스 초대 코드입니다.").attributes(experimental()),
			fieldWithPath("data.createdAt").type(JsonFieldType.NUMBER).description("워크스페이스 생성 시각입니다. Unix epoch seconds 기준입니다.").attributes(experimental()),
			fieldWithPath("data.updatedAt").type(JsonFieldType.NUMBER).description("워크스페이스 수정 시각입니다. Unix epoch seconds 기준입니다.").attributes(experimental()),
			fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
		};

		mockMvc.perform(get("/v1/workspaces/{workspaceId}", 2001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("workspaces-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(
					workspaceIdParameter
				),
				requestHeaders(
					authorizationHeader,
					acceptHeader
				),
				responseHeaders(
					responseContentTypeHeader
				),
				responseFields(
					responseFieldDescriptors
				),
				resource(ResourceSnippetParameters.builder()
					.tag("Workspaces")
					.responseSchema(schema("WorkspaceGetApiResponse"))
					.pathParameters(
						parameterWithName("workspaceId")
							.type(NUMBER)
							.description("조회할 워크스페이스 식별자입니다.")
					)
					.requestHeaders(
						authorizationHeader,
						acceptHeader
					)
					.responseHeaders(responseContentTypeHeader)
					.responseFields(responseFieldDescriptors)
					.build()
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
