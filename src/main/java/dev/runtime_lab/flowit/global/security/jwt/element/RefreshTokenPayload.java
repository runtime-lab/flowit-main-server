package dev.runtime_lab.flowit.global.security.jwt.element;

public record RefreshTokenPayload(
	String userId,
	Long tokenVersion
) {
}
