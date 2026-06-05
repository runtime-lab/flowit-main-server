package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.user.controller.UserController;
import dev.runtime_lab.flowit.domain.user.dto.UserUpdateRequest;
import dev.runtime_lab.flowit.domain.user.dto.UserUpdateResponse;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.service.UserMeService;
import dev.runtime_lab.flowit.domain.user.service.UserPasswordUpdateService;
import dev.runtime_lab.flowit.domain.user.service.UserProfileService;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class UserProfileUpdateApiDocsTest {

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
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(
				userMeService,
				userProfileService,
				userPasswordUpdateService,
				refreshTokenCookieService
			))
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
	void updateUser() throws Exception {
		String requestBody = """
			{
			  "nickname": "새로운닉네임"
			}
			""";

		when(userProfileService.update(any(CurrentUser.class), any(UserUpdateRequest.class)))
			.thenReturn(new UserUpdateResponse(
				1003L,
				"user@example.com",
				"새로운닉네임",
				UserStatus.ACTIVE,
				3001L,
				1779889000L
			));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andDo(document("users-me-update",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. ``Bearer {token}`` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.CONTENT_TYPE).description("요청 본문 미디어 타입입니다. ``application/json``을 사용합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				requestFields(
					fieldWithPath("nickname").type(JsonFieldType.STRING).description("변경할 닉네임입니다. 값이 있으면 공백일 수 없고 최대 100자까지 허용됩니다.").optional()
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("수정 후 현재 사용자 정보입니다."),
					fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("현재 사용자 식별자입니다."),
					fieldWithPath("data.email").type(JsonFieldType.STRING).description("현재 사용자 이메일입니다."),
					fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("변경된 닉네임입니다."),
					fieldWithPath("data.status").type(JsonFieldType.STRING).description("현재 사용자 상태입니다. link:enum-reference.html#user-status[UserStatus]를 참고합니다."),
					fieldWithPath("data.profileImageFileId").type(JsonFieldType.NUMBER).description("프로필 이미지 파일 식별자입니다. 등록된 이미지가 없으면 ``null``입니다.").optional(),
					fieldWithPath("data.updatedAt").type(JsonFieldType.NUMBER).description("사용자 정보 최종 수정 시각입니다. Unix epoch seconds 기준입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}

	@Test
	void updatePassword() throws Exception {
		String requestBody = """
			{
			  "currentPassword": "oldPassword",
			  "newPassword": "newPassword"
			}
			""";

		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/users/me/password")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("flowit_refresh_token=")))
			.andDo(document("users-password",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. ``Bearer {token}`` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.CONTENT_TYPE).description("요청 본문 미디어 타입입니다. ``application/json``을 사용합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다."),
					headerWithName(HttpHeaders.SET_COOKIE).description("만료 처리된 HttpOnly refresh token 쿠키입니다. 비밀번호 변경 후 클라이언트는 다시 로그인해야 합니다.")
				),
				requestFields(
					fieldWithPath("currentPassword").type(JsonFieldType.STRING).description("현재 평문 비밀번호입니다. 기존 비밀번호 해시와 일치해야 합니다."),
					fieldWithPath("newPassword").type(JsonFieldType.STRING).description("새 평문 비밀번호입니다. 비밀번호 정책을 통과해야 합니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("비어 있는 성공 응답입니다."),
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
