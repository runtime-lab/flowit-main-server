package dev.runtime_lab.flowit.domain.auth.service;

import dev.runtime_lab.flowit.domain.auth.dto.LogoutRequest;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthLogoutService {

	private final RefreshTokenService refreshTokenService;

	public void logout(LogoutRequest request) {
		refreshTokenService.revoke(request.refreshToken());
	}
}
