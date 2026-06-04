package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinByInviteCodeRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestResultResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestHistory;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceInviteCodeNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceJoinRequestInvalidTransitionException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceJoinRequestHistoryRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceJoinRequestRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.domain.workspace.statemachine.WorkspaceJoinStateMachineService;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceJoinRequestTransactionService {

	private final CurrentUserProvider currentUserProvider;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceJoinRequestRepository workspaceJoinRequestRepository;
	private final WorkspaceJoinRequestHistoryRepository workspaceJoinRequestHistoryRepository;
	private final WorkspaceJoinStateMachineService workspaceJoinStateMachineService;
	private final Clock clock;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long createInviteCodeRequest(CurrentUser currentUser, WorkspaceJoinByInviteCodeRequest request) {
		User requester = currentUserProvider.findActive(currentUser);
		Workspace workspace = workspaceRepository.findActiveByInviteCodeForUpdate(request.inviteCode())
			.orElseThrow(WorkspaceInviteCodeNotFoundException::new);
		Long now = now();

		WorkspaceJoinRequest joinRequest = workspaceJoinRequestRepository.save(
			WorkspaceJoinRequest.inviteCode(workspace, requester, request.inviteCode(), now)
		);
		WorkspaceJoinRequestHistory requestedHistory = joinRequest.recordRequested(requester, now);
		workspaceJoinRequestHistoryRepository.save(requestedHistory);

		return joinRequest.getId();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public WorkspaceJoinRequestResultResponse transition(Long joinRequestId, WorkspaceJoinRequestEvent event) {
		WorkspaceJoinRequest joinRequest = workspaceJoinRequestRepository.findByIdForUpdate(joinRequestId)
			.orElseThrow(WorkspaceJoinRequestInvalidTransitionException::new);

		workspaceJoinStateMachineService.send(joinRequest, event, joinRequest.getUser());

		return WorkspaceJoinRequestResultResponse.from(joinRequest);
	}

	private Long now() {
		return Instant.now(clock).getEpochSecond();
	}
}
