package dev.runtime_lab.flowit.domain.user.service;

import dev.runtime_lab.flowit.domain.user.dto.UserPasswordUpdateRequest;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.exception.InvalidCurrentPasswordException;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import dev.runtime_lab.flowit.global.security.password.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPasswordUpdateService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicy passwordPolicy;
	private final Clock clock;

	@Transactional
	public void update(CurrentUser currentUser, UserPasswordUpdateRequest request) {
		passwordPolicy.validate(request.newPassword());

		User user = userRepository.findActiveByIdForUpdate(currentUser.id())
			.filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedUserException::new);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new InvalidCurrentPasswordException();
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()), Instant.now(clock).getEpochSecond());
	}
}
