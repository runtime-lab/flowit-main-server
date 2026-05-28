package dev.runtime_lab.flowit.domain.auth.controller;

import dev.runtime_lab.flowit.domain.auth.dto.AuthTokenResult;
import dev.runtime_lab.flowit.domain.auth.dto.LoginRequest;
import dev.runtime_lab.flowit.domain.auth.dto.LogoutRequest;
import dev.runtime_lab.flowit.domain.auth.dto.TokenRefreshRequest;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidLoginCredentialsException;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidRefreshTokenException;
import dev.runtime_lab.flowit.domain.auth.service.AuthLoginService;
import dev.runtime_lab.flowit.domain.auth.service.AuthLogoutService;
import dev.runtime_lab.flowit.domain.auth.service.AuthTokenRefreshService;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenCookieService;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import dev.runtime_lab.flowit.global.security.password.InvalidPasswordPolicyException;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

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
	void setUp() {
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
			.build();
	}

	@Test
	void loginReturnsAccessTokenAndRefreshTokenCookie() throws Exception {
		String requestBody = """
			{
			  "email": "user@example.com",
			  "password": "plainPassword"
			}
			""";
		ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);

		when(authLoginService.login(any(LoginRequest.class)))
			.thenReturn(tokenResult("jwt-token", "refresh-token"));

		mockMvc.perform(post("/v1/public/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
				containsString("flowit_refresh_token=refresh-token"),
				containsString("Path=/v1/public/auth"),
				containsString("Max-Age=1209600"),
				containsString("HttpOnly"),
				containsString("SameSite=Lax"),
				not(containsString("Secure"))
			)))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").value(900L))
			.andExpect(jsonPath("$.data.refreshToken").doesNotExist())
			.andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1_209_600L))
			.andExpect(jsonPath("$.extensions").isMap());

		verify(authLoginService).login(requestCaptor.capture());
		assertEquals("user@example.com", requestCaptor.getValue().email());
		assertEquals("plainPassword", requestCaptor.getValue().password());
	}

	@Test
	void loginRejectsInvalidRequest() throws Exception {
		String requestBody = """
			{
			  "email": "not-email",
			  "password": "short"
			}
			""";

		mockMvc.perform(post("/v1/public/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
			.andExpect(jsonPath("$.extensions.fieldErrors").isArray());
	}

	@Test
	void loginReturnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
		String requestBody = """
			{
			  "email": "user@example.com",
			  "password": "plainPassword"
			}
			""";

		when(authLoginService.login(any(LoginRequest.class)))
			.thenThrow(new InvalidLoginCredentialsException());

		mockMvc.perform(post("/v1/public/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid email or password"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void loginReturnsBadRequestWhenPasswordContainsSpecialCharacter() throws Exception {
		String requestBody = """
			{
			  "email": "user@example.com",
			  "password": "plainPassword!"
			}
			""";

		when(authLoginService.login(any(LoginRequest.class)))
			.thenThrow(new InvalidPasswordPolicyException());

		mockMvc.perform(post("/v1/public/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
			.andExpect(jsonPath("$.error.message").value("Password must not contain special characters"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void refreshReturnsRotatedAccessTokenAndRefreshTokenCookie() throws Exception {
		ArgumentCaptor<TokenRefreshRequest> requestCaptor = ArgumentCaptor.forClass(TokenRefreshRequest.class);

		when(authTokenRefreshService.refresh(any(TokenRefreshRequest.class)))
			.thenReturn(tokenResult("new-access-token", "new-refresh-token"));

		mockMvc.perform(post("/v1/public/auth/refresh")
				.cookie(new Cookie("flowit_refresh_token", "old-refresh-token"))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
				containsString("flowit_refresh_token=new-refresh-token"),
				containsString("Path=/v1/public/auth"),
				containsString("Max-Age=1209600"),
				containsString("HttpOnly"),
				containsString("SameSite=Lax")
			)))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").value(900L))
			.andExpect(jsonPath("$.data.refreshToken").doesNotExist())
			.andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1_209_600L))
			.andExpect(jsonPath("$.extensions").isMap());

		verify(authTokenRefreshService).refresh(requestCaptor.capture());
		assertEquals("old-refresh-token", requestCaptor.getValue().refreshToken());
	}

	@Test
	void refreshReturnsUnauthorizedWhenCookieIsMissing() throws Exception {
		mockMvc.perform(post("/v1/public/auth/refresh")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid refresh token"))
			.andExpect(jsonPath("$.extensions").isMap());

		verifyNoInteractions(authTokenRefreshService);
	}

	@Test
	void refreshReturnsUnauthorizedWhenRefreshTokenIsInvalid() throws Exception {
		when(authTokenRefreshService.refresh(any(TokenRefreshRequest.class)))
			.thenThrow(new InvalidRefreshTokenException());

		mockMvc.perform(post("/v1/public/auth/refresh")
				.cookie(new Cookie("flowit_refresh_token", "invalid-refresh-token"))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH_401_001"))
			.andExpect(jsonPath("$.error.message").value("Invalid refresh token"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void logoutRevokesRefreshTokenAndExpiresCookie() throws Exception {
		ArgumentCaptor<LogoutRequest> requestCaptor = ArgumentCaptor.forClass(LogoutRequest.class);

		mockMvc.perform(post("/v1/public/auth/logout")
				.cookie(new Cookie("flowit_refresh_token", "refresh-token"))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
				containsString("flowit_refresh_token="),
				containsString("Path=/v1/public/auth"),
				containsString("Max-Age=0"),
				containsString("HttpOnly"),
				containsString("SameSite=Lax")
			)))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.extensions").isMap());

		verify(authLogoutService).logout(requestCaptor.capture());
		assertEquals("refresh-token", requestCaptor.getValue().refreshToken());
	}

	@Test
	void logoutExpiresCookieWhenCookieIsMissing() throws Exception {
		mockMvc.perform(post("/v1/public/auth/logout")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
				containsString("flowit_refresh_token="),
				containsString("Max-Age=0")
			)))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.extensions").isMap());

		verifyNoInteractions(authLogoutService);
	}

	private AuthTokenResult tokenResult(String accessToken, String refreshToken) {
		return new AuthTokenResult(
			JwtAccessToken.bearer(accessToken, 900L),
			new RefreshToken(refreshToken, 1_209_600L)
		);
	}
}
