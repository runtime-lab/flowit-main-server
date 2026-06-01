package dev.runtime_lab.flowit.domain.auth.controller;

import dev.runtime_lab.flowit.domain.auth.dto.AuthTokenResult;
import dev.runtime_lab.flowit.domain.auth.dto.LogoutRequest;
import dev.runtime_lab.flowit.domain.auth.dto.LoginRequest;
import dev.runtime_lab.flowit.domain.auth.dto.LoginResponse;
import dev.runtime_lab.flowit.domain.auth.dto.TokenRefreshRequest;
import dev.runtime_lab.flowit.domain.auth.dto.TokenRefreshResponse;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidRefreshTokenException;
import dev.runtime_lab.flowit.domain.auth.service.AuthenticationService;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenCookieService;
import dev.runtime_lab.flowit.global.web.response.ApiEmptyData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/public/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationService authenticationService;
	private final RefreshTokenCookieService refreshTokenCookieService;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthTokenResult tokenResult = authenticationService.login(request);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.create(tokenResult.refreshToken()).toString())
			.body(LoginResponse.from(tokenResult));
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenRefreshResponse> refresh(HttpServletRequest request) {
		TokenRefreshRequest tokenRefreshRequest = refreshTokenCookieService.resolve(request)
			.map(TokenRefreshRequest::new)
			.orElseThrow(InvalidRefreshTokenException::new);
		AuthTokenResult tokenResult = authenticationService.refresh(tokenRefreshRequest);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.create(tokenResult.refreshToken()).toString())
			.body(TokenRefreshResponse.from(tokenResult));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiEmptyData> logout(HttpServletRequest request) {
		refreshTokenCookieService.resolve(request)
			.map(LogoutRequest::new)
			.ifPresent(authenticationService::logout);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.expire().toString())
			.body(ApiEmptyData.empty());
	}
}
