package dev.runtime_lab.flowit.domain.auth.service;

import dev.runtime_lab.flowit.domain.auth.dto.AuthTokenResult;
import dev.runtime_lab.flowit.domain.auth.dto.TokenRefreshRequest;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidRefreshTokenException;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.global.security.jwt.FlowitJwtClaims;
import dev.runtime_lab.flowit.global.security.jwt.JwtTokenService;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenService;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshTokenPayload;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthTokenRefreshServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
	private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
	private final AuthTokenRefreshService authTokenRefreshService = new AuthTokenRefreshService(
		userRepository,
		jwtTokenService,
		refreshTokenService
	);

	@Test
	void refreshRotatesRefreshTokenAndIssuesNewTokens() {
		TokenRefreshRequest request = new TokenRefreshRequest("old-refresh-token");
		User user = User.builder()
			.id(1001L)
			.email("user@example.com")
			.passwordHash("encodedPassword")
			.tokenVersion(7L)
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.createdAt(1779888000L)
			.updatedAt(1779888000L)
			.build();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);

		when(refreshTokenService.consume("old-refresh-token")).thenReturn(Optional.of(new RefreshTokenPayload("1001", 7L)));
		when(userRepository.findById(1001L)).thenReturn(Optional.of(user));
		when(jwtTokenService.issueAccessToken(eq("1001"), anyMap()))
			.thenReturn(JwtAccessToken.bearer("new-access-token", 900L));
		when(refreshTokenService.issue("1001", 7L))
			.thenReturn(new RefreshToken("new-refresh-token", 1_209_600L));

		AuthTokenResult tokenResult = authTokenRefreshService.refresh(request);

		assertEquals("new-access-token", tokenResult.accessToken().tokenValue());
		assertEquals("Bearer", tokenResult.accessToken().tokenType());
		assertEquals(900L, tokenResult.accessToken().expiresIn());
		assertEquals("new-refresh-token", tokenResult.refreshToken().tokenValue());
		assertEquals(1_209_600L, tokenResult.refreshToken().expiresIn());
		verify(refreshTokenService).consume("old-refresh-token");
		verify(jwtTokenService).issueAccessToken(eq("1001"), claimsCaptor.capture());
		verify(refreshTokenService).issue("1001", 7L);
		assertEquals("user@example.com", claimsCaptor.getValue().get("email"));
		assertEquals("nickname", claimsCaptor.getValue().get("name"));
		assertEquals(7L, claimsCaptor.getValue().get(FlowitJwtClaims.TOKEN_VERSION));
	}

	@Test
	void refreshRejectsMissingRefreshToken() {
		TokenRefreshRequest request = new TokenRefreshRequest("missing-refresh-token");

		when(refreshTokenService.consume("missing-refresh-token")).thenReturn(Optional.empty());

		assertThrows(InvalidRefreshTokenException.class, () -> authTokenRefreshService.refresh(request));
		verify(userRepository, never()).findById(1001L);
		verify(jwtTokenService, never()).issueAccessToken(eq("1001"), anyMap());
		verify(refreshTokenService, never()).issue("1001", 7L);
	}

	@Test
	void refreshRejectsInactiveUser() {
		TokenRefreshRequest request = new TokenRefreshRequest("old-refresh-token");
		User user = User.builder()
			.id(1001L)
			.email("user@example.com")
			.passwordHash("encodedPassword")
			.name("nickname")
			.status(UserStatus.LOCKED)
			.createdAt(1779888000L)
			.updatedAt(1779888000L)
			.build();

		when(refreshTokenService.consume("old-refresh-token")).thenReturn(Optional.of(new RefreshTokenPayload("1001", 0L)));
		when(userRepository.findById(1001L)).thenReturn(Optional.of(user));

		assertThrows(InvalidRefreshTokenException.class, () -> authTokenRefreshService.refresh(request));
		verify(jwtTokenService, never()).issueAccessToken(eq("1001"), anyMap());
		verify(refreshTokenService, never()).issue("1001", 0L);
	}

	@Test
	void refreshRejectsTokenVersionMismatch() {
		TokenRefreshRequest request = new TokenRefreshRequest("old-refresh-token");
		User user = User.builder()
			.id(1001L)
			.email("user@example.com")
			.passwordHash("encodedPassword")
			.tokenVersion(8L)
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.createdAt(1779888000L)
			.updatedAt(1779888000L)
			.build();

		when(refreshTokenService.consume("old-refresh-token")).thenReturn(Optional.of(new RefreshTokenPayload("1001", 7L)));
		when(userRepository.findById(1001L)).thenReturn(Optional.of(user));

		assertThrows(InvalidRefreshTokenException.class, () -> authTokenRefreshService.refresh(request));
		verify(jwtTokenService, never()).issueAccessToken(eq("1001"), anyMap());
		verify(refreshTokenService, never()).issue("1001", 8L);
	}
}
