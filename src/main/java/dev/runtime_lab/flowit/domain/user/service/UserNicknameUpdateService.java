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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserNicknameUpdateService {

	private final UserRepository userRepository;
	private final Clock clock;

	@Transactional
	public UserNicknameUpdateResponse update(CurrentUser currentUser, UserNicknameUpdateRequest request) {
		User user = userRepository.findActiveByIdForUpdate(currentUser.id())
			.filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedUserException::new);

		user.changeNickname(request.nickname(), Instant.now(clock).getEpochSecond());

		return UserNicknameUpdateResponse.from(user);
	}
}
