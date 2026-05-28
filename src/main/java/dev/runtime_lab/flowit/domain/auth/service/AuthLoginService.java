package dev.runtime_lab.flowit.domain.auth.service;

import dev.runtime_lab.flowit.domain.auth.dto.AuthTokenResult;
import dev.runtime_lab.flowit.domain.auth.dto.LoginRequest;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidLoginCredentialsException;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.global.security.jwt.JwtTokenService;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenService;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import dev.runtime_lab.flowit.global.security.password.PasswordPolicy;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthLoginService {

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

		JwtAccessToken accessToken = jwtTokenService.issueAccessToken(
			String.valueOf(user.getId()),
			Map.of(
				"email", user.getEmail(),
				"name", user.getName()
			)
		);
		RefreshToken refreshToken = refreshTokenService.issue(String.valueOf(user.getId()));

		return new AuthTokenResult(accessToken, refreshToken);
	}
}
