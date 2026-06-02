package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMembersResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {

	private final UserRepository userRepository;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public WorkspaceMembersResponse members(CurrentUser currentUser, Long workspaceId) {
		User requester = findActiveCurrentUser(currentUser);
		Workspace workspace = workspaceRepository.findActiveById(workspaceId)
			.orElseThrow(WorkspaceNotFoundException::new);

		workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspace.getId(), requester.getId())
			.orElseThrow(() -> new WorkspaceMemberAccessDeniedException("Workspace membership is required."));

		List<WorkspaceMemberResponse> members = workspaceMemberRepository.findActiveMembersByWorkspaceId(workspace.getId());

		return new WorkspaceMembersResponse(workspace.getInviteCode(), members);
	}

	@Transactional
	public void remove(CurrentUser currentUser, Long workspaceId, Long userId) {
		User requester = findActiveCurrentUser(currentUser);

		if (Objects.equals(requester.getId(), userId)) {
			throw new WorkspaceMemberAccessDeniedException();
		}

		Workspace workspace = workspaceRepository.findActiveByIdForUpdate(workspaceId)
				.orElseThrow(WorkspaceNotFoundException::new);

		WorkspaceMember requesterMembership = workspaceMemberRepository
			.findActiveByWorkspaceIdAndUserIdForUpdate(workspace.getId(), requester.getId())
			.orElseThrow(WorkspaceMemberAccessDeniedException::new);

		if (!requesterMembership.getRole().canRemoveMember()) {
			throw new WorkspaceMemberAccessDeniedException();
		}

		WorkspaceMember targetMembership = workspaceMemberRepository
			.findActiveByWorkspaceIdAndUserIdForUpdate(workspace.getId(), userId)
			.orElseThrow(WorkspaceMemberNotFoundException::new);

		if (targetMembership.getRole().isWorkspaceOwner()) {
			throw new WorkspaceMemberAccessDeniedException();
		}

		targetMembership.softDelete(Instant.now(clock).getEpochSecond());
	}

	private User findActiveCurrentUser(CurrentUser currentUser) {
		return userRepository.findActiveById(currentUser.id())
			.filter(user -> user.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedUserException::new);
	}
}
