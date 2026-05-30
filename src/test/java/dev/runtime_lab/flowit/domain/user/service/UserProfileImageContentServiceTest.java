package dev.runtime_lab.flowit.domain.user.service;

import dev.runtime_lab.flowit.domain.file.entity.FileMetadata;
import dev.runtime_lab.flowit.domain.file.exception.ProfileImageNotFoundException;
import dev.runtime_lab.flowit.domain.file.storage.LocalProfileImageStorage;
import dev.runtime_lab.flowit.domain.file.storage.ProfileImageFileContent;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageContentResponse;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserProfileImageContentServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final LocalProfileImageStorage localProfileImageStorage = mock(LocalProfileImageStorage.class);
	private final UserProfileImageContentService service = new UserProfileImageContentService(
		userRepository,
		localProfileImageStorage
	);

	@Test
	void getReturnsCurrentUserProfileImageContent() {
		byte[] bytes = new byte[] {1, 2, 3};
		FileMetadata profileImageFile = FileMetadata.builder()
			.id(3001L)
			.storageKey("users/1/avatar.png")
			.originalFilename("avatar.png")
			.contentType("image/png")
			.sizeBytes(3L)
			.width(1)
			.height(1)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.profileImageFile(profileImageFile)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));
		when(localProfileImageStorage.load("users/1/avatar.png")).thenReturn(new ProfileImageFileContent(bytes));

		UserProfileImageContentResponse response = service.get(new CurrentUser(1L, "user@example.com", "nickname"));

		assertEquals("image/png", response.contentType());
		assertEquals(3L, response.contentLength());
		assertArrayEquals(bytes, response.bytes());
		verify(userRepository).findActiveById(1L);
		verify(localProfileImageStorage).load("users/1/avatar.png");
	}

	@Test
	void getRejectsMissingUser() {
		when(userRepository.findActiveById(1L)).thenReturn(Optional.empty());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> service.get(new CurrentUser(1L, "user@example.com", "nickname"))
		);
		verifyNoInteractions(localProfileImageStorage);
	}

	@Test
	void getRejectsInactiveUser() {
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.status(UserStatus.LOCKED)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> service.get(new CurrentUser(1L, "user@example.com", "nickname"))
		);
		verifyNoInteractions(localProfileImageStorage);
	}

	@Test
	void getRejectsUserWithoutProfileImage() {
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

		assertThrows(
			ProfileImageNotFoundException.class,
			() -> service.get(new CurrentUser(1L, "user@example.com", "nickname"))
		);
		verifyNoInteractions(localProfileImageStorage);
	}
}
