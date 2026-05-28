package dev.runtime_lab.flowit.domain.auth.service;

import dev.runtime_lab.flowit.domain.auth.dto.LogoutRequest;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthLogoutServiceTest {

	private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
	private final AuthLogoutService authLogoutService = new AuthLogoutService(refreshTokenService);

	@Test
	void logoutRevokesRefreshToken() {
		authLogoutService.logout(new LogoutRequest("refresh-token"));

		verify(refreshTokenService).revoke("refresh-token");
	}
}
