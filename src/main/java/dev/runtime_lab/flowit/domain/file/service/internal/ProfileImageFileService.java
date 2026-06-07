package dev.runtime_lab.flowit.domain.file.service.internal;

import dev.runtime_lab.flowit.domain.file.entity.FileMetadata;
import dev.runtime_lab.flowit.domain.file.repository.FileMetadataRepository;
import dev.runtime_lab.flowit.domain.file.storage.LocalProfileImageStorage;
import dev.runtime_lab.flowit.domain.file.storage.ProfileImageFileContent;
import dev.runtime_lab.flowit.domain.file.storage.StoredProfileImageFile;
import dev.runtime_lab.flowit.global.stereotype.InternalService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@InternalService
@RequiredArgsConstructor
public class ProfileImageFileService {

	private final FileMetadataRepository fileMetadataRepository;
	private final LocalProfileImageStorage localProfileImageStorage;
	private final Clock clock;

	// TODO: user <-> file persistence boundary 재설계
	// TODO: 이미지 플로우 contract 기반 분리 + JPA 매핑 + 테스트 작업 편성

	@Transactional
	public FileMetadata store(Long userId, MultipartFile imageFile) {
		StoredProfileImageFile storedFile = localProfileImageStorage.store(userId, imageFile);
		registerNewFileRollbackCleanup(storedFile.storageKey());

		long now = Instant.now(clock).getEpochSecond();
		return fileMetadataRepository.save(
			FileMetadata.builder()
				.storageKey(storedFile.storageKey())
				.originalFilename(storedFile.originalFilename())
				.contentType(storedFile.contentType())
				.sizeBytes(storedFile.sizeBytes())
				.width(storedFile.width())
				.height(storedFile.height())
				.createdAt(now)
				.updatedAt(now)
				.build()
		);
	}

	@Transactional
	public void deleteAfterCommit(FileMetadata fileMetadata) {
		String storageKey = fileMetadata.getStorageKey();
		fileMetadataRepository.delete(fileMetadata);
		registerOldFileDeletionAfterCommit(storageKey);
	}

	public ProfileImageFileContent load(FileMetadata fileMetadata) {
		return localProfileImageStorage.load(fileMetadata.getStorageKey());
	}

	private void registerNewFileRollbackCleanup(String storageKey) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_COMMITTED) {
					return;
				}
				localProfileImageStorage.deleteIfExists(storageKey);
			}
		});
	}

	private void registerOldFileDeletionAfterCommit(String storageKey) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			localProfileImageStorage.deleteIfExists(storageKey);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

			@Override
			public void afterCommit() {
				try {
					localProfileImageStorage.deleteIfExists(storageKey);
				}
				catch (RuntimeException exception) {
					log.warn("Failed to delete previous profile image after commit: {}", storageKey, exception);
				}
			}
		});
	}
}
