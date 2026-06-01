package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceInviteCodeGenerationException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

	private static final int MAX_INVITE_CODE_GENERATION_ATTEMPTS = 10;

	private final UserRepository userRepository;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceInviteCodeGenerator workspaceInviteCodeGenerator;
	private final Clock clock;

	@Transactional
	public WorkspaceCreateResponse create(CurrentUser currentUser, WorkspaceCreateRequest request) {
		User creator = findActiveCurrentUser(currentUser);

		long now = Instant.now(clock).getEpochSecond();
		Workspace workspace = workspaceRepository.save(Workspace.builder()
			.name(request.name())
			.description(request.description())
			.inviteCode(generateUniqueInviteCode())
			.createdBy(creator)
			.createdAt(now)
			.updatedAt(now)
			.build());

		workspaceMemberRepository.save(WorkspaceMember.builder()
			.workspace(workspace)
			.user(creator)
			.role(WorkspaceMemberRole.OWNER)
			.joinedAt(now)
			.createdAt(now)
			.updatedAt(now)
			.build());

		return WorkspaceCreateResponse.from(workspace);
	}

	@Transactional
	public void delete(CurrentUser currentUser, Long workspaceId) {
		User user = findActiveCurrentUser(currentUser);

		Workspace workspace = workspaceRepository.findActiveByIdForUpdate(workspaceId)
			.orElseThrow(WorkspaceNotFoundException::new);

		if (!workspaceMemberRepository.existsActiveOwnerByWorkspaceAndUser(workspace, user)) {
			throw new WorkspaceAccessDeniedException();
		}

		Long deletedAt = Instant.now(clock).getEpochSecond();
		workspace.softDelete(deletedAt);
		workspaceMemberRepository.softDeleteActiveByWorkspaceId(workspace.getId(), deletedAt);
	}

	private User findActiveCurrentUser(CurrentUser currentUser) {
		return userRepository.findActiveById(currentUser.id())
			.filter(user -> user.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedUserException::new);
	}

	private String generateUniqueInviteCode() {
		for (int attempt = 0; attempt < MAX_INVITE_CODE_GENERATION_ATTEMPTS; attempt++) {
			String inviteCode = workspaceInviteCodeGenerator.generate();
			if (!workspaceRepository.existsByInviteCode(inviteCode)) {
				return inviteCode;
			}
		}

		throw new WorkspaceInviteCodeGenerationException();
	}
}
