package dev.runtime_lab.flowit.global.security.jwt;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenVersionJwtValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(
		OAuth2ErrorCodes.INVALID_TOKEN,
		"Access token has an invalid token version.",
		null
	);

	private final UserRepository userRepository;

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		Long userId = parseLong(token.getSubject());
		Long tokenVersion = claimAsLong(token.getClaims().get(FlowitJwtClaims.TOKEN_VERSION));
		if (userId == null || tokenVersion == null) {
			return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
		}

		return userRepository.findActiveById(userId)
			.filter(user -> user.getStatus() == UserStatus.ACTIVE)
			.filter(user -> hasMatchingTokenVersion(user, tokenVersion))
			.map(user -> OAuth2TokenValidatorResult.success())
			.orElseGet(() -> OAuth2TokenValidatorResult.failure(INVALID_TOKEN));
	}

	private boolean hasMatchingTokenVersion(User user, Long tokenVersion) {
		return Objects.equals(user.getTokenVersion(), tokenVersion);
	}

	private Long parseLong(String value) {
		if (value == null) {
			return null;
		}

		try {
			return Long.valueOf(value);
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

	private Long claimAsLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}

		if (value instanceof String stringValue) {
			return parseLong(stringValue);
		}

		return null;
	}
}
