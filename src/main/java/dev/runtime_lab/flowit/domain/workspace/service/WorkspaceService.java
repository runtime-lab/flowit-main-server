package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceUpdateRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.exception.*;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.MEMBERSHIP_REQUIRED;
import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.WORKSPACE_UPDATE_NOT_ALLOWED;
import static dev.runtime_lab.flowit.domain.workspace.policy.WorkspaceInviteCodePolicy.MAX_GENERATION_ATTEMPTS;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

	private final CurrentUserProvider currentUserProvider;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceInviteCodeGenerator workspaceInviteCodeGenerator;
	private final Clock clock;

	@Transactional
	public WorkspaceCreateResponse create(CurrentUser currentUser, WorkspaceCreateRequest request) {
		User creator = currentUserProvider.findActive(currentUser);

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
	public WorkspaceResponse update(CurrentUser currentUser, Long workspaceId, WorkspaceUpdateRequest request) {
		User requester = currentUserProvider.findActive(currentUser);

		Workspace workspace = workspaceRepository.findActiveByIdForUpdate(workspaceId)
				.orElseThrow(WorkspaceNotFoundException::new);

		WorkspaceMember requesterMembership = workspaceMemberRepository
				.findActiveByWorkspaceIdAndUserIdForUpdate(workspace.getId(), requester.getId())
				.orElseThrow(() -> new WorkspaceMemberAccessDeniedException(MEMBERSHIP_REQUIRED));

		if (!requesterMembership.getRole().canUpdateWorkspace()) {
			throw new WorkspaceMemberAccessDeniedException(WORKSPACE_UPDATE_NOT_ALLOWED);
		}

		boolean isChanged = false;

		if (request.name() != null) {
			if (!StringUtils.hasText(request.name())) {
				throw new InvalidWorkspaceUpdateException();
			}

			workspace.setWorkspaceName(request.name());
			isChanged = true;
		}

		if (request.description() != null) {
			workspace.setWorkspaceDescription(request.description().isBlank() ? null : request.description());
			isChanged = true;
		}

		if (isChanged) {
			workspace.setUpdatedAt(Instant.now(clock).getEpochSecond());
		}

		return WorkspaceResponse.from(workspace);
	}

	@Transactional(readOnly = true)
	public WorkspaceResponse get(CurrentUser currentUser, Long workspaceId) {
		User requester = currentUserProvider.findActive(currentUser);
		Workspace workspace = workspaceRepository.findActiveById(workspaceId)
			.orElseThrow(WorkspaceNotFoundException::new);

		workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspace.getId(), requester.getId())
			.orElseThrow(() -> new WorkspaceMemberAccessDeniedException(MEMBERSHIP_REQUIRED));

		return WorkspaceResponse.from(workspace);
	}

	@Transactional
	public void delete(CurrentUser currentUser, Long workspaceId) {
		User user = currentUserProvider.findActive(currentUser);

		Workspace workspace = workspaceRepository.findActiveByIdForUpdate(workspaceId)
			.orElseThrow(WorkspaceNotFoundException::new);

		if (!workspaceMemberRepository.existsActiveOwnerByWorkspaceAndUser(workspace, user)) {
			throw new WorkspaceAccessDeniedException();
		}

		Long deletedAt = Instant.now(clock).getEpochSecond();

		workspace.softDelete(deletedAt);

		workspaceMemberRepository.softDeleteActiveByWorkspaceId(workspace.getId(), deletedAt);
	}

	private String generateUniqueInviteCode() {
		for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
			String inviteCode = workspaceInviteCodeGenerator.generate();
			if (!workspaceRepository.existsByInviteCode(inviteCode)) {
				return inviteCode;
			}
		}

		throw new WorkspaceInviteCodeGenerationException();
	}
}
