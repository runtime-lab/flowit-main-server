package dev.runtime_lab.flowit.global.security.jwt.element;

public record JwtAccessToken(
	String tokenValue,
	String tokenType,
	long expiresIn
) {

	public static JwtAccessToken bearer(String tokenValue, long expiresIn) {
		return new JwtAccessToken(tokenValue, "Bearer", expiresIn);
	}
}
