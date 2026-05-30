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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileImageContentService {

	private final UserRepository userRepository;
	private final LocalProfileImageStorage localProfileImageStorage;

	@Transactional(readOnly = true)
	public UserProfileImageContentResponse get(CurrentUser currentUser) {
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
}
