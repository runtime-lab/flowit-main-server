package dev.runtime_lab.flowit.global.security.jwt;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RefreshTokenCookieService {

	private final JwtProperties jwtProperties;

	public ResponseCookie create(RefreshToken refreshToken) {
		return baseCookie(refreshToken.tokenValue())
			.maxAge(Duration.ofSeconds(refreshToken.expiresIn()))
			.build();
	}

	public ResponseCookie expire() {
		return baseCookie("")
			.maxAge(Duration.ZERO)
			.build();
	}

	public Optional<String> resolve(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}

		return Arrays.stream(cookies)
			.filter(cookie -> jwtProperties.refreshTokenCookieName().equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(StringUtils::hasText)
			.findFirst();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(jwtProperties.refreshTokenCookieName(), value)
			.httpOnly(true)
			.secure(jwtProperties.refreshTokenCookieSecure())
			.path(jwtProperties.refreshTokenCookiePath())
			.sameSite(jwtProperties.refreshTokenCookieSameSite());
	}
}
