package dev.runtime_lab.flowit.domain.user.service;

import dev.runtime_lab.flowit.domain.user.dto.JoinRequest;
import dev.runtime_lab.flowit.domain.user.dto.JoinResponse;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.exception.DuplicateActiveEmailException;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.global.security.password.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserJoinService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final PasswordPolicy passwordPolicy;

	@Transactional
	public JoinResponse join(JoinRequest request) {
		passwordPolicy.validate(request.passwordPlan());

		if (userRepository.existsActiveByEmail(request.email())) {
			throw new DuplicateActiveEmailException(request.email());
		}

		long now = Instant.now(clock).getEpochSecond();
		User user = User.builder()
			.email(request.email())
			.passwordHash(passwordEncoder.encode(request.passwordPlan()))
			.name(request.nickname())
			.createdAt(now)
			.updatedAt(now)
			.build();

		return JoinResponse.from(userRepository.save(user));
	}
}
