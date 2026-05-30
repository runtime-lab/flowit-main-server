package dev.runtime_lab.flowit.domain.user.service;

import dev.runtime_lab.flowit.domain.user.dto.UserPasswordUpdateRequest;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.exception.InvalidCurrentPasswordException;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import dev.runtime_lab.flowit.global.security.password.InvalidPasswordPolicyException;
import dev.runtime_lab.flowit.global.security.password.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPasswordUpdateServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final PasswordPolicy passwordPolicy = new PasswordPolicy();
	private final Clock clock = Clock.fixed(Instant.parse("2026-05-30T12:00:00Z"), ZoneOffset.UTC);
	private final UserPasswordUpdateService userPasswordUpdateService = new UserPasswordUpdateService(
		userRepository,
		passwordEncoder,
		passwordPolicy,
		clock
	);

	@Test
	void updateChangesPasswordAndIncrementsTokenVersion() {
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("old-hash")
			.tokenVersion(7L)
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("oldPassword", "old-hash")).thenReturn(true);
		when(passwordEncoder.encode("newPassword")).thenReturn("new-hash");

		userPasswordUpdateService.update(
			new CurrentUser(1L, "claim@example.com", "claim-name"),
			new UserPasswordUpdateRequest("oldPassword", "newPassword")
		);

		assertEquals("new-hash", user.getPasswordHash());
		assertEquals(8L, user.getTokenVersion());
		assertEquals(1_780_142_400L, user.getUpdatedAt());
		verify(userRepository).findActiveByIdForUpdate(1L);
		verify(passwordEncoder).matches("oldPassword", "old-hash");
		verify(passwordEncoder).encode("newPassword");
	}

	@Test
	void updateRejectsMissingUser() {
		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.empty());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userPasswordUpdateService.update(
				new CurrentUser(1L, "user@example.com", "nickname"),
				new UserPasswordUpdateRequest("oldPassword", "newPassword")
			)
		);
		verify(userRepository).findActiveByIdForUpdate(1L);
		verify(passwordEncoder, never()).matches("oldPassword", "old-hash");
	}

	@Test
	void updateRejectsInactiveUser() {
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("old-hash")
			.name("nickname")
			.status(UserStatus.LOCKED)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userPasswordUpdateService.update(
				new CurrentUser(1L, "user@example.com", "nickname"),
				new UserPasswordUpdateRequest("oldPassword", "newPassword")
			)
		);
		verify(passwordEncoder, never()).matches("oldPassword", "old-hash");
	}

	@Test
	void updateRejectsCurrentPasswordMismatch() {
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("old-hash")
			.tokenVersion(7L)
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrongPassword", "old-hash")).thenReturn(false);

		assertThrows(
			InvalidCurrentPasswordException.class,
			() -> userPasswordUpdateService.update(
				new CurrentUser(1L, "user@example.com", "nickname"),
				new UserPasswordUpdateRequest("wrongPassword", "newPassword")
			)
		);
		assertEquals("old-hash", user.getPasswordHash());
		assertEquals(7L, user.getTokenVersion());
		verify(passwordEncoder, never()).encode("newPassword");
	}

	@Test
	void updateRejectsNewPasswordContainingSpecialCharacterBeforeRepositoryLookup() {
		assertThrows(
			InvalidPasswordPolicyException.class,
			() -> userPasswordUpdateService.update(
				new CurrentUser(1L, "user@example.com", "nickname"),
				new UserPasswordUpdateRequest("oldPassword", "newPassword!")
			)
		);
		verify(userRepository, never()).findActiveByIdForUpdate(1L);
	}
}
