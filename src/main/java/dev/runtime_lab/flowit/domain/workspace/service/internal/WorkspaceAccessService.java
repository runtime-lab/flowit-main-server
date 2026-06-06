package dev.runtime_lab.flowit.domain.workspace.service.internal;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceAccessContext;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.stereotype.InternalService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.MEMBERSHIP_REQUIRED;

@InternalService
@RequiredArgsConstructor
public class WorkspaceAccessService {

	private final CurrentUserProvider currentUserProvider;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional(readOnly = true)
	public WorkspaceAccessContext resolveMemberAccess(CurrentUser currentUser, Long workspaceId) {
		User requester = currentUserProvider.findActive(currentUser);
		Workspace workspace = workspaceRepository.findActiveById(workspaceId)
			.orElseThrow(WorkspaceNotFoundException::new);
		WorkspaceMember membership = workspaceMemberRepository
			.findActiveByWorkspaceIdAndUserId(workspace.getId(), requester.getId())
			.orElseThrow(() -> new WorkspaceMemberAccessDeniedException(MEMBERSHIP_REQUIRED));

		return new WorkspaceAccessContext(requester, workspace, membership);
	}

	@Transactional(readOnly = true)
	public Optional<WorkspaceMember> findActiveMember(Long workspaceId, Long memberId) {
		return workspaceMemberRepository.findActiveByWorkspaceIdAndMemberId(workspaceId, memberId);
	}
}
