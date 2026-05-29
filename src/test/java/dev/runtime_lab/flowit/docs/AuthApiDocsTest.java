package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.auth.controller.AuthController;
import dev.runtime_lab.flowit.domain.auth.dto.AuthTokenResult;
import dev.runtime_lab.flowit.domain.auth.dto.LoginRequest;
import dev.runtime_lab.flowit.domain.auth.dto.TokenRefreshRequest;
import dev.runtime_lab.flowit.domain.auth.service.AuthLoginService;
import dev.runtime_lab.flowit.domain.auth.service.AuthLogoutService;
import dev.runtime_lab.flowit.domain.auth.service.AuthTokenRefreshService;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenCookieService;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class AuthApiDocsTest {

	private static final String DUMMY_LOGIN_ACCESS_TOKEN =
		"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMDAxIiwidG9rZW5fdHlwZSI6ImFjY2VzcyJ9.invalid-signature";
	private static final String DUMMY_REFRESHED_ACCESS_TOKEN =
		"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMDAxIiwidG9rZW5fdHlwZSI6ImFjY2VzcyIsInJvdGF0ZWQiOnRydWV9.invalid-signature";

	private final AuthLoginService authLoginService = mock(AuthLoginService.class);
	private final AuthTokenRefreshService authTokenRefreshService = mock(AuthTokenRefreshService.class);
	private final AuthLogoutService authLogoutService = mock(AuthLogoutService.class);
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
			.standaloneSetup(new AuthController(
				authLoginService,
				authTokenRefreshService,
				authLogoutService,
				refreshTokenCookieService
			))
			.setControllerAdvice(new ApiResponseBodyAdvice(), new GlobalExceptionHandler())
			.setValidator(validator)
			.apply(documentationConfiguration(restDocumentation))
			.build();
	}

	@Test
	void login() throws Exception {
		String requestBody = """
			{
			  "email": "user@example.com",
			  "password": "plainPassword"
			}
			""";

		when(authLoginService.login(any(LoginRequest.class)))
			.thenReturn(tokenResult(DUMMY_LOGIN_ACCESS_TOKEN, "refresh-token"));

		mockMvc.perform(post("/v1/public/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("flowit_refresh_token=refresh-token")))
			.andDo(document("auth-login",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("요청 본문 미디어 타입입니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다."),
					headerWithName(HttpHeaders.SET_COOKIE).description("HttpOnly refresh token 쿠키입니다.")
				),
				requestFields(
					fieldWithPath("email").type(JsonFieldType.STRING).description("로그인 이메일입니다."),
					fieldWithPath("password").type(JsonFieldType.STRING).description("평문 비밀번호입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("로그인 토큰 응답입니다."),
					fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("Bearer access token입니다."),
					fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입입니다."),
					fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER).description("Access token 만료까지 남은 초입니다."),
					fieldWithPath("data.refreshTokenExpiresIn").type(JsonFieldType.NUMBER).description("Refresh token 쿠키 만료까지 남은 초입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}

	@Test
	void refresh() throws Exception {
		when(authTokenRefreshService.refresh(any(TokenRefreshRequest.class)))
			.thenReturn(tokenResult(DUMMY_REFRESHED_ACCESS_TOKEN, "new-refresh-token"));

		mockMvc.perform(post("/v1/public/auth/refresh")
				.cookie(new Cookie("flowit_refresh_token", "old-refresh-token"))
				.header(HttpHeaders.COOKIE, "flowit_refresh_token=old-refresh-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("flowit_refresh_token=new-refresh-token")))
			.andDo(document("auth-refresh",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				requestCookies(
					cookieWithName("flowit_refresh_token").description("로그인 때 발급된 refresh token 쿠키입니다.")
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다."),
					headerWithName(HttpHeaders.SET_COOKIE).description("새로 발급된 refresh token 쿠키입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("재발급 토큰 응답입니다."),
					fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("새 Bearer access token입니다."),
					fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입입니다."),
					fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER).description("Access token 만료까지 남은 초입니다."),
					fieldWithPath("data.refreshTokenExpiresIn").type(JsonFieldType.NUMBER).description("Refresh token 쿠키 만료까지 남은 초입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}

	@Test
	void logout() throws Exception {
		mockMvc.perform(post("/v1/public/auth/logout")
				.cookie(new Cookie("flowit_refresh_token", "refresh-token"))
				.header(HttpHeaders.COOKIE, "flowit_refresh_token=refresh-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("flowit_refresh_token=")))
			.andDo(document("auth-logout",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				requestCookies(
					cookieWithName("flowit_refresh_token").description("로그인 때 발급된 refresh token 쿠키입니다.")
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다."),
					headerWithName(HttpHeaders.SET_COOKIE).description("만료 처리된 refresh token 쿠키입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("비어 있는 성공 응답입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}

	private AuthTokenResult tokenResult(String accessToken, String refreshToken) {
		return new AuthTokenResult(
			JwtAccessToken.bearer(accessToken, 900L),
			new RefreshToken(refreshToken, 1_209_600L)
		);
	}
}

