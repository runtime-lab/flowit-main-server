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
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthTokenRefreshService {

	private final UserRepository userRepository;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;

	@Transactional(readOnly = true)
	public AuthTokenResult refresh(TokenRefreshRequest request) {
		RefreshTokenPayload refreshTokenPayload = refreshTokenService.consume(request.refreshToken())
			.orElseThrow(InvalidRefreshTokenException::new);
		User user = findActiveUser(refreshTokenPayload.userId());
		if (!Objects.equals(user.getTokenVersion(), refreshTokenPayload.tokenVersion())) {
			throw new InvalidRefreshTokenException();
		}

		JwtAccessToken accessToken = jwtTokenService.issueAccessToken(
			String.valueOf(user.getId()),
			Map.of(
				"email", user.getEmail(),
				"name", user.getName(),
				FlowitJwtClaims.TOKEN_VERSION, user.getTokenVersion()
			)
		);
		RefreshToken refreshToken = refreshTokenService.issue(String.valueOf(user.getId()), user.getTokenVersion());

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
