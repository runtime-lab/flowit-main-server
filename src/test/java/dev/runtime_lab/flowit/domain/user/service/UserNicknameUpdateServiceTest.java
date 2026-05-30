package dev.runtime_lab.flowit.domain.user.service;

import dev.runtime_lab.flowit.domain.user.dto.UserNicknameUpdateRequest;
import dev.runtime_lab.flowit.domain.user.dto.UserNicknameUpdateResponse;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserNicknameUpdateServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final Clock clock = Clock.fixed(Instant.parse("2026-05-30T12:00:00Z"), ZoneOffset.UTC);
	private final UserNicknameUpdateService userNicknameUpdateService = new UserNicknameUpdateService(userRepository, clock);

	@Test
	void updateChangesCurrentUserNickname() {
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));

		UserNicknameUpdateResponse response = userNicknameUpdateService.update(
			new CurrentUser(1L, "claim@example.com", "claim-name"),
			new UserNicknameUpdateRequest("new-nickname")
		);

		assertEquals("new-nickname", user.getName());
		assertEquals(1_780_142_400L, user.getUpdatedAt());
		assertEquals(1L, response.id());
		assertEquals("user@example.com", response.email());
		assertEquals("new-nickname", response.nickname());
		assertEquals(UserStatus.ACTIVE, response.status());
		assertEquals(1_780_142_400L, response.updatedAt());
		verify(userRepository).findActiveByIdForUpdate(1L);
	}

	@Test
	void updateRejectsMissingUser() {
		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.empty());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userNicknameUpdateService.update(
				new CurrentUser(1L, "user@example.com", "nickname"),
				new UserNicknameUpdateRequest("new-nickname")
			)
		);
		verify(userRepository).findActiveByIdForUpdate(1L);
	}

	@Test
	void updateRejectsInactiveUser() {
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.status(UserStatus.LOCKED)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userNicknameUpdateService.update(
				new CurrentUser(1L, "user@example.com", "nickname"),
				new UserNicknameUpdateRequest("new-nickname")
			)
		);
	}
}
