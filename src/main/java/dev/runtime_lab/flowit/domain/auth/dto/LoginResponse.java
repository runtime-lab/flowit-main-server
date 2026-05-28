package dev.runtime_lab.flowit.domain.auth.dto;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;

public record LoginResponse(
	String accessToken,
	String tokenType,
	long expiresIn,
	long refreshTokenExpiresIn
) {

	public static LoginResponse from(AuthTokenResult tokenResult) {
		JwtAccessToken accessToken = tokenResult.accessToken();
		RefreshToken refreshToken = tokenResult.refreshToken();

		return new LoginResponse(
			accessToken.tokenValue(),
			accessToken.tokenType(),
			accessToken.expiresIn(),
			refreshToken.expiresIn()
		);
	}
}
