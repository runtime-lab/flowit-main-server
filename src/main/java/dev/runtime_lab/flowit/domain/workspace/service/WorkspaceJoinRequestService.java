package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinByInviteCodeRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinDetailResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestResultResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinDetailsResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceJoinRequestRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.JOIN_REQUEST_HISTORY_ACCESS_DENIED;

@Service
@RequiredArgsConstructor
public class WorkspaceJoinRequestService {

	private final CurrentUserProvider currentUserProvider;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceJoinRequestRepository workspaceJoinRequestRepository;
	private final WorkspaceJoinRequestTransactionService workspaceJoinRequestTransactionService;
	private final WorkspaceJoinRequestFailureRecorder workspaceJoinRequestFailureRecorder;

	public WorkspaceJoinRequestResultResponse joinByInviteCode(
		CurrentUser currentUser,
		WorkspaceJoinByInviteCodeRequest request
	) {
		Long joinRequestId = null;

		try {
			joinRequestId = workspaceJoinRequestTransactionService.createInviteCodeRequest(currentUser, request);
			workspaceJoinRequestTransactionService.transition(joinRequestId, WorkspaceJoinRequestEvent.MARK_READY);
			workspaceJoinRequestTransactionService.transition(joinRequestId, WorkspaceJoinRequestEvent.APPROVE);
			return workspaceJoinRequestTransactionService.transition(joinRequestId, WorkspaceJoinRequestEvent.JOIN);
		}
		catch (RuntimeException exception) {
			recordFailure(joinRequestId, exception);
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public WorkspaceJoinDetailsResponse requests(CurrentUser currentUser, Long workspaceId) {
		User requester = currentUserProvider.findActive(currentUser);
		Workspace workspace = workspaceRepository.findActiveById(workspaceId)
			.orElseThrow(WorkspaceNotFoundException::new);
		WorkspaceMember requesterMembership = workspaceMemberRepository
			.findActiveByWorkspaceIdAndUserId(workspace.getId(), requester.getId())
			.orElseThrow(() -> new WorkspaceMemberAccessDeniedException(JOIN_REQUEST_HISTORY_ACCESS_DENIED));

		if (!requesterMembership.getRole().canManageJoinRequests()) {
			throw new WorkspaceMemberAccessDeniedException(JOIN_REQUEST_HISTORY_ACCESS_DENIED);
		}

		return new WorkspaceJoinDetailsResponse(
			workspaceJoinRequestRepository.findByWorkspaceIdWithHistories(workspace.getId()).stream()
				.map(WorkspaceJoinDetailResponse::from)
				.toList()
		);
	}

	private void recordFailure(Long joinRequestId, RuntimeException exception) {
		if (joinRequestId == null) {
			return;
		}

		try {
			workspaceJoinRequestFailureRecorder.record(joinRequestId, exception);
		}
		catch (RuntimeException failureRecordingException) {
			exception.addSuppressed(failureRecordingException);
		}
	}
}
