package dev.runtime_lab.flowit.domain.user.service;

import dev.runtime_lab.flowit.domain.file.entity.FileMetadata;
import dev.runtime_lab.flowit.domain.file.exception.ProfileImageNotFoundException;
import dev.runtime_lab.flowit.domain.file.repository.FileMetadataRepository;
import dev.runtime_lab.flowit.domain.file.storage.LocalProfileImageStorage;
import dev.runtime_lab.flowit.domain.file.storage.ProfileImageFileContent;
import dev.runtime_lab.flowit.domain.file.storage.StoredProfileImageFile;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageContentResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageUpdateResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserUpdateRequest;
import dev.runtime_lab.flowit.domain.user.dto.UserUpdateResponse;
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
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserProfileServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final FileMetadataRepository fileMetadataRepository = mock(FileMetadataRepository.class);
	private final LocalProfileImageStorage localProfileImageStorage = mock(LocalProfileImageStorage.class);
	private final Clock clock = Clock.fixed(Instant.parse("2026-05-30T12:00:00Z"), ZoneOffset.UTC);
	private final UserProfileService service = new UserProfileService(
		userRepository,
		fileMetadataRepository,
		localProfileImageStorage,
		clock
	);

	@Test
	void updateChangesCurrentUserNickname() {
		User user = activeUser(null);
		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));

		UserUpdateResponse response = service.update(
			new CurrentUser(1L, "claim@example.com", "claim-name"),
			new UserUpdateRequest("new-nickname")
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
			() -> service.update(
				new CurrentUser(1L, "user@example.com", "nickname"),
				new UserUpdateRequest("new-nickname")
			)
		);
		verify(userRepository).findActiveByIdForUpdate(1L);
	}

	@Test
	void updateRejectsInactiveUser() {
		User user = activeUser(null, UserStatus.LOCKED);
		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> service.update(
				new CurrentUser(1L, "user@example.com", "nickname"),
				new UserUpdateRequest("new-nickname")
			)
		);
	}

	@Test
	void replaceProfileImageCreatesNewFileMetadataAndDeletesPreviousRow() {
		MultipartFile multipartFile = mock(MultipartFile.class);
		FileMetadata oldFileMetadata = fileMetadata(2001L, "users/1/old.png");
		FileMetadata savedFileMetadata = fileMetadata(3001L, "users/1/new.png");
		User user = activeUser(oldFileMetadata);
		ArgumentCaptor<FileMetadata> fileMetadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);

		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));
		when(localProfileImageStorage.store(1L, multipartFile))
			.thenReturn(new StoredProfileImageFile("users/1/new.png", "avatar.png", "image/png", 68L, 1, 1));
		when(fileMetadataRepository.save(fileMetadataCaptor.capture())).thenReturn(savedFileMetadata);

		UserProfileImageUpdateResponse response = service.replaceProfileImage(
			new CurrentUser(1L, "user@example.com", "nickname"),
			multipartFile
		);

		FileMetadata fileMetadataToSave = fileMetadataCaptor.getValue();
		assertEquals("users/1/new.png", fileMetadataToSave.getStorageKey());
		assertEquals("avatar.png", fileMetadataToSave.getOriginalFilename());
		assertEquals("image/png", fileMetadataToSave.getContentType());
		assertEquals(68L, fileMetadataToSave.getSizeBytes());
		assertEquals(1, fileMetadataToSave.getWidth());
		assertEquals(1, fileMetadataToSave.getHeight());
		assertEquals(1_780_142_400_000L, fileMetadataToSave.getCreatedAt());
		assertEquals(1_780_142_400_000L, fileMetadataToSave.getUpdatedAt());
		assertSame(savedFileMetadata, user.getProfileImageFile());
		assertEquals(1_780_142_400L, user.getUpdatedAt());
		assertEquals(3001L, response.fileId());

		verify(fileMetadataRepository).delete(oldFileMetadata);
		verify(localProfileImageStorage).deleteIfExists("users/1/old.png");
	}

	@Test
	void replaceProfileImageRejectsMissingUserBeforeWritingFile() {
		MultipartFile multipartFile = mock(MultipartFile.class);
		when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.empty());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> service.replaceProfileImage(new CurrentUser(1L, "user@example.com", "nickname"), multipartFile)
		);

		verify(localProfileImageStorage, never()).store(any(), any());
		verify(fileMetadataRepository, never()).save(any());
	}

	@Test
	void getProfileImageReturnsCurrentUserProfileImageContent() {
		byte[] bytes = new byte[] {1, 2, 3};
		FileMetadata profileImageFile = fileMetadata(3001L, "users/1/avatar.png");
		User user = activeUser(profileImageFile);

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));
		when(localProfileImageStorage.load("users/1/avatar.png")).thenReturn(new ProfileImageFileContent(bytes));

		UserProfileImageContentResponse response = service.getProfileImage(
			new CurrentUser(1L, "user@example.com", "nickname")
		);

		assertEquals("image/png", response.contentType());
		assertEquals(3L, response.contentLength());
		assertArrayEquals(bytes, response.bytes());
		verify(userRepository).findActiveById(1L);
		verify(localProfileImageStorage).load("users/1/avatar.png");
	}

	@Test
	void getProfileImageRejectsMissingUser() {
		when(userRepository.findActiveById(1L)).thenReturn(Optional.empty());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> service.getProfileImage(new CurrentUser(1L, "user@example.com", "nickname"))
		);
		verifyNoInteractions(localProfileImageStorage);
	}

	@Test
	void getProfileImageRejectsInactiveUser() {
		User user = activeUser(null, UserStatus.LOCKED);
		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> service.getProfileImage(new CurrentUser(1L, "user@example.com", "nickname"))
		);
		verifyNoInteractions(localProfileImageStorage);
	}

	@Test
	void getProfileImageRejectsUserWithoutProfileImage() {
		User user = activeUser(null);
		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

		assertThrows(
			ProfileImageNotFoundException.class,
			() -> service.getProfileImage(new CurrentUser(1L, "user@example.com", "nickname"))
		);
		verifyNoInteractions(localProfileImageStorage);
	}

	private User activeUser(FileMetadata profileImageFile) {
		return activeUser(profileImageFile, UserStatus.ACTIVE);
	}

	private User activeUser(FileMetadata profileImageFile, UserStatus status) {
		return User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.profileImageFile(profileImageFile)
			.status(status)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private FileMetadata fileMetadata(Long id, String storageKey) {
		return FileMetadata.builder()
			.id(id)
			.storageKey(storageKey)
			.originalFilename("avatar.png")
			.contentType("image/png")
			.sizeBytes(68L)
			.width(1)
			.height(1)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
