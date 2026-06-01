package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.user.controller.UserController;
import dev.runtime_lab.flowit.domain.user.dto.UserMeResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserMeWorkspaceResponse;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.service.UserMeService;
import dev.runtime_lab.flowit.domain.user.service.UserPasswordUpdateService;
import dev.runtime_lab.flowit.domain.user.service.UserProfileService;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUserArgumentResolver;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenCookieService;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
import java.time.Duration;
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

import static dev.runtime_lab.flowit.docs.support.ResponseFieldStability.experimental;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class UserMeApiDocsTest {

	private final UserMeService userMeService = mock(UserMeService.class);
	private final UserProfileService userProfileService = mock(UserProfileService.class);
	private final UserPasswordUpdateService userPasswordUpdateService = mock(UserPasswordUpdateService.class);
	private final RefreshTokenCookieService refreshTokenCookieService = new RefreshTokenCookieService(
		new JwtProperties(
			"flowit-test",
			Duration.ofMinutes(15),
			Duration.ofDays(14),
			"flowit_refresh_token",
			"/v1/public/auth",
			"Lax",
			false,
			null,
			null,
			null,
			null
		)
	);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(
				userMeService,
				userProfileService,
				userPasswordUpdateService,
				refreshTokenCookieService
			))
			.setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
			.setControllerAdvice(new ApiResponseBodyAdvice(), new GlobalExceptionHandler())
			.apply(documentationConfiguration(restDocumentation))
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void me() throws Exception {
		UserMeResponse response = new UserMeResponse(
			1003L,
			"user@example.com",
			"nickname",
			UserStatus.ACTIVE,
			3001L,
			"/v1/users/me/profile-image",
			List.of(new UserMeWorkspaceResponse(2001L, "Flowit", "Team workspace", 3L, WorkspaceMemberRole.OWNER, 1779889000L)),
			List.of()
		);

		when(userMeService.getMe(any(CurrentUser.class))).thenReturn(response);
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("users-me",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. `Bearer {token}` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("현재 사용자와 워크스페이스 정보입니다."),
					fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("현재 사용자 식별자입니다."),
					fieldWithPath("data.email").type(JsonFieldType.STRING).description("현재 사용자 이메일입니다."),
					fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("현재 사용자 닉네임입니다."),
					fieldWithPath("data.status").type(JsonFieldType.STRING).description("현재 사용자 상태입니다. link:enum-reference.html#user-status[UserStatus]를 참고합니다."),
					fieldWithPath("data.profileImageFileId").type(JsonFieldType.NUMBER).description("프로필 이미지 파일 식별자입니다. 등록된 이미지가 없으면 `null`입니다.").optional(),
					fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 바이너리를 조회할 수 있는 URL입니다. 등록된 이미지가 없으면 `null`입니다.").optional().attributes(experimental()),
					fieldWithPath("data.workspaces").type(JsonFieldType.ARRAY).description("현재 사용자가 속한 워크스페이스 목록입니다."),
					fieldWithPath("data.workspaces[].id").type(JsonFieldType.NUMBER).description("워크스페이스 식별자입니다."),
					fieldWithPath("data.workspaces[].name").type(JsonFieldType.STRING).description("워크스페이스 이름입니다."),
					fieldWithPath("data.workspaces[].description").type(JsonFieldType.STRING).description("워크스페이스 설명입니다.").optional(),
					fieldWithPath("data.workspaces[].memberCount").type(JsonFieldType.NUMBER).description("워크스페이스의 활성 멤버 수입니다."),
					fieldWithPath("data.workspaces[].role").type(JsonFieldType.STRING).description("해당 워크스페이스에서 현재 사용자의 권한입니다. link:enum-reference.html#workspace-member-role[WorkspaceMemberRole]을 참고합니다."),
					fieldWithPath("data.workspaces[].joinedAt").type(JsonFieldType.NUMBER).description("워크스페이스 참여 시각입니다. Unix epoch seconds 기준입니다."),
					fieldWithPath("data.notificationAlerts").type(JsonFieldType.ARRAY).description("현재 사용자에게 표시할 알림 목록입니다. 알림 모델링 전까지는 빈 배열입니다.").attributes(experimental()),
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
