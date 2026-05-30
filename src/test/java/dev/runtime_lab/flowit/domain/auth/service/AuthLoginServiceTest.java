package dev.runtime_lab.flowit.domain.auth.service;

import dev.runtime_lab.flowit.domain.auth.dto.LoginRequest;
import dev.runtime_lab.flowit.domain.auth.dto.AuthTokenResult;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidLoginCredentialsException;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.global.security.jwt.FlowitJwtClaims;
import dev.runtime_lab.flowit.global.security.jwt.JwtTokenService;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenService;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import dev.runtime_lab.flowit.global.security.password.InvalidPasswordPolicyException;
import dev.runtime_lab.flowit.global.security.password.PasswordPolicy;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthLoginServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
	private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
	private final PasswordPolicy passwordPolicy = new PasswordPolicy();
	private final AuthLoginService authLoginService = new AuthLoginService(
		userRepository,
		passwordEncoder,
		jwtTokenService,
		refreshTokenService,
		passwordPolicy
	);

	@Test
	void loginIssuesAccessAndRefreshToken() {
		LoginRequest request = new LoginRequest("user@example.com", "plainPassword");
		User user = User.builder()
			.id(1001L)
			.email("user@example.com")
			.passwordHash("encodedPassword")
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.createdAt(1779888000L)
			.updatedAt(1779888000L)
			.build();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);

		when(userRepository.findActiveByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("plainPassword", "encodedPassword")).thenReturn(true);
		when(jwtTokenService.issueAccessToken(eq("1001"), anyMap()))
			.thenReturn(JwtAccessToken.bearer("jwt-token", 900L));
		when(refreshTokenService.issue("1001", 0L))
			.thenReturn(new RefreshToken("refresh-token", 1_209_600L));

		AuthTokenResult tokenResult = authLoginService.login(request);

		assertEquals("jwt-token", tokenResult.accessToken().tokenValue());
		assertEquals("Bearer", tokenResult.accessToken().tokenType());
		assertEquals(900L, tokenResult.accessToken().expiresIn());
		assertEquals("refresh-token", tokenResult.refreshToken().tokenValue());
		assertEquals(1_209_600L, tokenResult.refreshToken().expiresIn());
		verify(jwtTokenService).issueAccessToken(eq("1001"), claimsCaptor.capture());
		verify(refreshTokenService).issue("1001", 0L);
		assertEquals("user@example.com", claimsCaptor.getValue().get("email"));
		assertEquals("nickname", claimsCaptor.getValue().get("name"));
		assertEquals(0L, claimsCaptor.getValue().get(FlowitJwtClaims.TOKEN_VERSION));
	}

	@Test
	void loginRejectsMissingUser() {
		LoginRequest request = new LoginRequest("user@example.com", "plainPassword");

		when(userRepository.findActiveByEmail("user@example.com")).thenReturn(Optional.empty());

		assertThrows(InvalidLoginCredentialsException.class, () -> authLoginService.login(request));
		verify(passwordEncoder, never()).matches("plainPassword", "encodedPassword");
		verify(jwtTokenService, never()).issueAccessToken(eq("1001"), anyMap());
		verify(refreshTokenService, never()).issue("1001", 0L);
	}

	@Test
	void loginRejectsInactiveUser() {
		LoginRequest request = new LoginRequest("user@example.com", "plainPassword");
		User user = User.builder()
			.id(1001L)
			.email("user@example.com")
			.passwordHash("encodedPassword")
			.name("nickname")
			.status(UserStatus.LOCKED)
			.createdAt(1779888000L)
			.updatedAt(1779888000L)
			.build();

		when(userRepository.findActiveByEmail("user@example.com")).thenReturn(Optional.of(user));

		assertThrows(InvalidLoginCredentialsException.class, () -> authLoginService.login(request));
		verify(passwordEncoder, never()).matches("plainPassword", "encodedPassword");
		verify(jwtTokenService, never()).issueAccessToken(eq("1001"), anyMap());
		verify(refreshTokenService, never()).issue("1001", 0L);
	}

	@Test
	void loginRejectsPasswordMismatch() {
		LoginRequest request = new LoginRequest("user@example.com", "plainPassword");
		User user = User.builder()
			.id(1001L)
			.email("user@example.com")
			.passwordHash("encodedPassword")
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.createdAt(1779888000L)
			.updatedAt(1779888000L)
			.build();

		when(userRepository.findActiveByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("plainPassword", "encodedPassword")).thenReturn(false);

		assertThrows(InvalidLoginCredentialsException.class, () -> authLoginService.login(request));
		verify(jwtTokenService, never()).issueAccessToken(eq("1001"), anyMap());
		verify(refreshTokenService, never()).issue("1001", 0L);
	}

	@Test
	void loginRejectsPasswordContainingSpecialCharacterBeforeRepositoryLookup() {
		LoginRequest request = new LoginRequest("user@example.com", "plainPassword!");

		assertThrows(InvalidPasswordPolicyException.class, () -> authLoginService.login(request));
		verify(userRepository, never()).findActiveByEmail(anyString());
		verify(passwordEncoder, never()).matches(anyString(), anyString());
		verify(jwtTokenService, never()).issueAccessToken(eq("1001"), anyMap());
		verify(refreshTokenService, never()).issue("1001", 0L);
	}
}
