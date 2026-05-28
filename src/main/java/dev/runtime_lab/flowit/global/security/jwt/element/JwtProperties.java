package dev.runtime_lab.flowit.global.security.jwt.element;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowit.security.jwt")
public record JwtProperties(
	String issuer,
	Duration accessTokenTimeToLive,
	Duration refreshTokenTimeToLive,
	String refreshTokenCookieName,
	String refreshTokenCookiePath,
	String refreshTokenCookieSameSite,
	boolean refreshTokenCookieSecure,
	String privateKeyLocation,
	String publicKeyLocation,
	String privateKey,
	String publicKey
) {

	public JwtProperties {
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("flowit.security.jwt.issuer must not be blank");
		}
		if (accessTokenTimeToLive == null || accessTokenTimeToLive.isZero() || accessTokenTimeToLive.isNegative()) {
			throw new IllegalArgumentException("flowit.security.jwt.access-token-time-to-live must be positive");
		}
		if (refreshTokenTimeToLive == null || refreshTokenTimeToLive.isZero() || refreshTokenTimeToLive.isNegative()) {
			throw new IllegalArgumentException("flowit.security.jwt.refresh-token-time-to-live must be positive");
		}
		if (refreshTokenCookieName == null || refreshTokenCookieName.isBlank()) {
			throw new IllegalArgumentException("flowit.security.jwt.refresh-token-cookie-name must not be blank");
		}
		if (refreshTokenCookiePath == null || refreshTokenCookiePath.isBlank()) {
			throw new IllegalArgumentException("flowit.security.jwt.refresh-token-cookie-path must not be blank");
		}
		if (refreshTokenCookieSameSite == null || refreshTokenCookieSameSite.isBlank()) {
			throw new IllegalArgumentException("flowit.security.jwt.refresh-token-cookie-same-site must not be blank");
		}
	}
}
