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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

	private final UserRepository userRepository;
	private final FileMetadataRepository fileMetadataRepository;
	private final LocalProfileImageStorage localProfileImageStorage;
	private final Clock clock;

	@Transactional
	public UserUpdateResponse update(CurrentUser currentUser, UserUpdateRequest request) {
		User user = userRepository.findActiveByIdForUpdate(currentUser.id())
			.filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedUserException::new);

		Long updatedAt = Instant.now(clock).getEpochSecond();
		if (request.nickname() != null) {
			user.changeNickname(request.nickname(), updatedAt);
		}

		return UserUpdateResponse.from(user);
	}

	@Transactional
	public UserProfileImageUpdateResponse replaceProfileImage(CurrentUser currentUser, MultipartFile imageFile) {
		User user = userRepository.findActiveByIdForUpdate(currentUser.id())
			.filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedUserException::new);

		StoredProfileImageFile storedFile = localProfileImageStorage.store(user.getId(), imageFile);
		registerNewFileRollbackCleanup(storedFile.storageKey());

		Instant now = Instant.now(clock);
		FileMetadata newFileMetadata = fileMetadataRepository.save(
			FileMetadata.builder()
				.storageKey(storedFile.storageKey())
				.originalFilename(storedFile.originalFilename())
				.contentType(storedFile.contentType())
				.sizeBytes(storedFile.sizeBytes())
				.width(storedFile.width())
				.height(storedFile.height())
				.createdAt(now.toEpochMilli())
				.updatedAt(now.toEpochMilli())
				.build()
		);

		FileMetadata oldFileMetadata = user.replaceProfileImageFile(newFileMetadata, now.getEpochSecond());
		if (oldFileMetadata != null) {
			String oldStorageKey = oldFileMetadata.getStorageKey();
			fileMetadataRepository.delete(oldFileMetadata);
			registerOldFileDeletionAfterCommit(oldStorageKey);
		}

		return UserProfileImageUpdateResponse.from(newFileMetadata);
	}

	@Transactional(readOnly = true)
	public UserProfileImageContentResponse getProfileImage(CurrentUser currentUser) {
		User user = userRepository.findActiveById(currentUser.id())
			.filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedUserException::new);

		FileMetadata profileImageFile = user.getProfileImageFile();
		if (profileImageFile == null) {
			throw new ProfileImageNotFoundException();
		}

		ProfileImageFileContent content = localProfileImageStorage.load(profileImageFile.getStorageKey());
		return new UserProfileImageContentResponse(profileImageFile.getContentType(), content.bytes());
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
