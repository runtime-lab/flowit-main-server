package dev.runtime_lab.flowit.domain.auth.service;

import dev.runtime_lab.flowit.domain.auth.dto.AuthTokenResult;
import dev.runtime_lab.flowit.domain.auth.dto.LogoutRequest;
import dev.runtime_lab.flowit.domain.auth.dto.LoginRequest;
import dev.runtime_lab.flowit.domain.auth.dto.TokenRefreshRequest;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidLoginCredentialsException;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidRefreshTokenException;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.global.security.jwt.FlowitJwtClaims;
import dev.runtime_lab.flowit.global.security.jwt.JwtTokenService;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenService;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshTokenPayload;
import dev.runtime_lab.flowit.global.security.password.PasswordPolicy;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;
	private final PasswordPolicy passwordPolicy;

	@Transactional(readOnly = true)
	public AuthTokenResult login(LoginRequest request) {
		passwordPolicy.validate(request.password());

		User user = userRepository.findActiveByEmail(request.email())
			.filter(activeUser -> activeUser.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(InvalidLoginCredentialsException::new);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidLoginCredentialsException();
		}

		return issueTokens(user);
	}

	@Transactional(readOnly = true)
	public AuthTokenResult refresh(TokenRefreshRequest request) {
		RefreshTokenPayload refreshTokenPayload = refreshTokenService.consume(request.refreshToken())
			.orElseThrow(InvalidRefreshTokenException::new);
		User user = findActiveUser(refreshTokenPayload.userId());
		if (!Objects.equals(user.getTokenVersion(), refreshTokenPayload.tokenVersion())) {
			throw new InvalidRefreshTokenException();
		}

		return issueTokens(user);
	}

	public void logout(LogoutRequest request) {
		refreshTokenService.revoke(request.refreshToken());
	}

	private AuthTokenResult issueTokens(User user) {
		String userId = String.valueOf(user.getId());
		JwtAccessToken accessToken = jwtTokenService.issueAccessToken(
			userId,
			Map.of(
				"email", user.getEmail(),
				"name", user.getName(),
				FlowitJwtClaims.TOKEN_VERSION, user.getTokenVersion()
			)
		);
		RefreshToken refreshToken = refreshTokenService.issue(userId, user.getTokenVersion());

		return new AuthTokenResult(accessToken, refreshToken);
	}

	private User findActiveUser(String userId) {
		try {
			return userRepository.findById(Long.valueOf(userId))
				.filter(user -> user.getDeletedAt() == null)
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(InvalidRefreshTokenException::new);
		}
		catch (NumberFormatException exception) {
			throw new InvalidRefreshTokenException();
		}
	}
}
