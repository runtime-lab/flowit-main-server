package dev.runtime_lab.flowit.global.security.jwt;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshTokenCookieServiceTest {

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

	@Test
	void createBuildsHttpOnlyRefreshTokenCookie() {
		ResponseCookie cookie = refreshTokenCookieService.create(new RefreshToken("refresh-token", 1_209_600L));
		String setCookie = cookie.toString();

		assertTrue(setCookie.contains("flowit_refresh_token=refresh-token"));
		assertTrue(setCookie.contains("Path=/v1/public/auth"));
		assertTrue(setCookie.contains("Max-Age=1209600"));
		assertTrue(setCookie.contains("HttpOnly"));
		assertTrue(setCookie.contains("SameSite=Lax"));
	}

	@Test
	void expireBuildsExpiredRefreshTokenCookie() {
		ResponseCookie cookie = refreshTokenCookieService.expire();
		String setCookie = cookie.toString();

		assertTrue(setCookie.contains("flowit_refresh_token="));
		assertTrue(setCookie.contains("Path=/v1/public/auth"));
		assertTrue(setCookie.contains("Max-Age=0"));
		assertTrue(setCookie.contains("HttpOnly"));
		assertTrue(setCookie.contains("SameSite=Lax"));
	}

	@Test
	void resolveReadsConfiguredCookieFromRequest() {
		HttpServletRequest request = mock(HttpServletRequest.class);

		when(request.getCookies()).thenReturn(new Cookie[] {
			new Cookie("other", "other-token"),
			new Cookie("flowit_refresh_token", "refresh-token")
		});

		Optional<String> refreshToken = refreshTokenCookieService.resolve(request);

		assertTrue(refreshToken.isPresent());
		assertEquals("refresh-token", refreshToken.get());
	}

	@Test
	void resolveReturnsEmptyWhenCookieIsMissing() {
		HttpServletRequest request = mock(HttpServletRequest.class);

		when(request.getCookies()).thenReturn(new Cookie[] {
			new Cookie("other", "other-token")
		});

		Optional<String> refreshToken = refreshTokenCookieService.resolve(request);

		assertTrue(refreshToken.isEmpty());
	}
}
