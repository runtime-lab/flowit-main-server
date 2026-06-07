package dev.runtime_lab.flowit.domain.file.service.internal;

import dev.runtime_lab.flowit.domain.file.entity.FileMetadata;
import dev.runtime_lab.flowit.domain.file.repository.FileMetadataRepository;
import dev.runtime_lab.flowit.domain.file.storage.LocalProfileImageStorage;
import dev.runtime_lab.flowit.domain.file.storage.ProfileImageFileContent;
import dev.runtime_lab.flowit.domain.file.storage.StoredProfileImageFile;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileImageFileServiceTest {

	private final FileMetadataRepository fileMetadataRepository = mock(FileMetadataRepository.class);
	private final LocalProfileImageStorage localProfileImageStorage = mock(LocalProfileImageStorage.class);
	private final Clock clock = Clock.fixed(Instant.parse("2026-05-30T12:00:00Z"), ZoneOffset.UTC);
	private final ProfileImageFileService profileImageFileService = new ProfileImageFileService(
		fileMetadataRepository,
		localProfileImageStorage,
		clock
	);

	@Test
	void storeCreatesFileMetadataFromStoredProfileImageFile() {
		MultipartFile multipartFile = mock(MultipartFile.class);
		FileMetadata savedFileMetadata = fileMetadata(3001L, "users/1/new.png");
		ArgumentCaptor<FileMetadata> fileMetadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);

		when(localProfileImageStorage.store(1L, multipartFile))
			.thenReturn(new StoredProfileImageFile("users/1/new.png", "avatar.png", "image/png", 68L, 1, 1));
		when(fileMetadataRepository.save(fileMetadataCaptor.capture())).thenReturn(savedFileMetadata);

		FileMetadata result = profileImageFileService.store(1L, multipartFile);

		FileMetadata fileMetadataToSave = fileMetadataCaptor.getValue();
		assertSame(savedFileMetadata, result);
		assertEquals("users/1/new.png", fileMetadataToSave.getStorageKey());
		assertEquals("avatar.png", fileMetadataToSave.getOriginalFilename());
		assertEquals("image/png", fileMetadataToSave.getContentType());
		assertEquals(68L, fileMetadataToSave.getSizeBytes());
		assertEquals(1, fileMetadataToSave.getWidth());
		assertEquals(1, fileMetadataToSave.getHeight());
		assertEquals(1_780_142_400L, fileMetadataToSave.getCreatedAt());
		assertEquals(1_780_142_400L, fileMetadataToSave.getUpdatedAt());
	}

	@Test
	void storeDeletesNewStorageFileWhenTransactionRollsBack() {
		MultipartFile multipartFile = mock(MultipartFile.class);
		RuntimeException saveFailure = new RuntimeException("save failed");

		when(localProfileImageStorage.store(1L, multipartFile))
			.thenReturn(new StoredProfileImageFile("users/1/new.png", "avatar.png", "image/png", 68L, 1, 1));
		when(fileMetadataRepository.save(org.mockito.ArgumentMatchers.any(FileMetadata.class)))
			.thenThrow(saveFailure);

		TransactionSynchronizationManager.initSynchronization();
		try {
			RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> profileImageFileService.store(1L, multipartFile)
			);

			assertSame(saveFailure, exception);
			verify(localProfileImageStorage, never()).deleteIfExists("users/1/new.png");
			TransactionSynchronizationManager.getSynchronizations()
				.forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
			verify(localProfileImageStorage).deleteIfExists("users/1/new.png");
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void deleteAfterCommitDeletesMetadataAndStorageImmediatelyWithoutSynchronization() {
		FileMetadata fileMetadata = fileMetadata(2001L, "users/1/old.png");

		profileImageFileService.deleteAfterCommit(fileMetadata);

		verify(fileMetadataRepository).delete(fileMetadata);
		verify(localProfileImageStorage).deleteIfExists("users/1/old.png");
	}

	@Test
	void loadDelegatesToStorageByStorageKey() {
		FileMetadata fileMetadata = fileMetadata(3001L, "users/1/avatar.png");
		byte[] bytes = new byte[] {1, 2, 3};

		when(localProfileImageStorage.load("users/1/avatar.png")).thenReturn(new ProfileImageFileContent(bytes));

		ProfileImageFileContent content = profileImageFileService.load(fileMetadata);

		assertArrayEquals(bytes, content.bytes());
		verify(localProfileImageStorage).load("users/1/avatar.png");
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
