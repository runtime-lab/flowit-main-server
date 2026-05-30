package dev.runtime_lab.flowit.global.security.jwt;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenVersionJwtValidatorTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final TokenVersionJwtValidator tokenVersionJwtValidator = new TokenVersionJwtValidator(userRepository);

	@Test
	void validateSucceedsWhenUserTokenVersionMatchesClaim() {
		when(userRepository.findActiveById(1001L)).thenReturn(Optional.of(user(UserStatus.ACTIVE, 7L)));

		OAuth2TokenValidatorResult result = tokenVersionJwtValidator.validate(jwt("1001", 7L));

		assertFalse(result.hasErrors());
		verify(userRepository).findActiveById(1001L);
	}

	@Test
	void validateFailsWhenTokenVersionDoesNotMatch() {
		when(userRepository.findActiveById(1001L)).thenReturn(Optional.of(user(UserStatus.ACTIVE, 8L)));

		OAuth2TokenValidatorResult result = tokenVersionJwtValidator.validate(jwt("1001", 7L));

		assertTrue(result.hasErrors());
	}

	@Test
	void validateFailsWhenTokenVersionClaimIsMissing() {
		OAuth2TokenValidatorResult result = tokenVersionJwtValidator.validate(jwtWithoutTokenVersion("1001"));

		assertTrue(result.hasErrors());
	}

	@Test
	void validateFailsWhenSubjectIsInvalid() {
		OAuth2TokenValidatorResult result = tokenVersionJwtValidator.validate(jwt("not-number", 7L));

		assertTrue(result.hasErrors());
	}

	@Test
	void validateFailsWhenUserIsInactive() {
		when(userRepository.findActiveById(1001L)).thenReturn(Optional.of(user(UserStatus.LOCKED, 7L)));

		OAuth2TokenValidatorResult result = tokenVersionJwtValidator.validate(jwt("1001", 7L));

		assertTrue(result.hasErrors());
	}

	private User user(UserStatus status, Long tokenVersion) {
		return User.builder()
			.id(1001L)
			.email("user@example.com")
			.passwordHash("hash")
			.tokenVersion(tokenVersion)
			.name("nickname")
			.status(status)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private Jwt jwt(String subject, Long tokenVersion) {
		return Jwt.withTokenValue("token")
			.header("alg", "none")
			.subject(subject)
			.claim(FlowitJwtClaims.TOKEN_VERSION, tokenVersion)
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
			.build();
	}

	private Jwt jwtWithoutTokenVersion(String subject) {
		return Jwt.withTokenValue("token")
			.header("alg", "none")
			.subject(subject)
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
			.build();
	}
}
