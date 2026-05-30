package dev.runtime_lab.flowit.global.security.jwt;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshTokenPayload;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

	private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
	@SuppressWarnings("unchecked")
	private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
	private final JwtProperties jwtProperties = new JwtProperties(
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
	);
	private final RefreshTokenService refreshTokenService = new RefreshTokenService(stringRedisTemplate, jwtProperties);

	@BeforeEach
	void setUp() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Test
	void issueStoresHashedRefreshTokenWithTtl() {
		RefreshToken refreshToken = refreshTokenService.issue("1001", 7L);

		assertFalse(refreshToken.tokenValue().isBlank());
		assertEquals(1_209_600L, refreshToken.expiresIn());
		verify(valueOperations).set(
			refreshTokenService.keyOf(refreshToken.tokenValue()),
			"1001:7",
			Duration.ofDays(14)
		);
	}

	@Test
	void consumeReturnsPayloadAndDeletesToken() {
		when(valueOperations.getAndDelete(refreshTokenService.keyOf("refresh-token")))
			.thenReturn("1001:7");

		Optional<RefreshTokenPayload> payload = refreshTokenService.consume("refresh-token");

		assertTrue(payload.isPresent());
		assertEquals("1001", payload.get().userId());
		assertEquals(7L, payload.get().tokenVersion());
		verify(valueOperations).getAndDelete(refreshTokenService.keyOf("refresh-token"));
	}

	@Test
	void consumeReturnsEmptyWhenTokenDoesNotExist() {
		when(valueOperations.getAndDelete(refreshTokenService.keyOf("refresh-token")))
			.thenReturn(null);

		Optional<RefreshTokenPayload> payload = refreshTokenService.consume("refresh-token");

		assertTrue(payload.isEmpty());
	}

	@Test
	void consumeReturnsEmptyWhenPayloadIsInvalid() {
		when(valueOperations.getAndDelete(refreshTokenService.keyOf("refresh-token")))
			.thenReturn("1001");

		Optional<RefreshTokenPayload> payload = refreshTokenService.consume("refresh-token");

		assertTrue(payload.isEmpty());
	}

	@Test
	void revokeDeletesToken() {
		when(stringRedisTemplate.delete(refreshTokenService.keyOf("refresh-token")))
			.thenReturn(true);

		boolean revoked = refreshTokenService.revoke("refresh-token");

		assertTrue(revoked);
		verify(stringRedisTemplate).delete(refreshTokenService.keyOf("refresh-token"));
	}

	@Test
	void revokeReturnsFalseWhenTokenDoesNotExist() {
		when(stringRedisTemplate.delete(refreshTokenService.keyOf("refresh-token")))
			.thenReturn(false);

		boolean revoked = refreshTokenService.revoke("refresh-token");

		assertFalse(revoked);
	}
}
