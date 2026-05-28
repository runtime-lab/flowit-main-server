package dev.runtime_lab.flowit.domain.auth.dto;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;

public record AuthTokenResult(
	JwtAccessToken accessToken,
	RefreshToken refreshToken
) {
}
