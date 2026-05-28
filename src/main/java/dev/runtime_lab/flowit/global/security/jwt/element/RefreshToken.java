package dev.runtime_lab.flowit.global.security.jwt.element;

public record RefreshToken(
	String tokenValue,
	long expiresIn
) {
}
