package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinByInviteCodeRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestResultResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestsResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestMethod;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceJoinRequestRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceJoinRequestServiceTest {

	private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
	private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
	private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
	private final WorkspaceJoinRequestRepository workspaceJoinRequestRepository = mock(WorkspaceJoinRequestRepository.class);
	private final WorkspaceJoinRequestTransactionService transactionService =
		mock(WorkspaceJoinRequestTransactionService.class);
	private final WorkspaceJoinRequestFailureRecorder failureRecorder = mock(WorkspaceJoinRequestFailureRecorder.class);
	private final WorkspaceJoinRequestService service = new WorkspaceJoinRequestService(
		currentUserProvider,
		workspaceRepository,
		workspaceMemberRepository,
		workspaceJoinRequestRepository,
		transactionService,
		failureRecorder
	);

	@Test
	void joinByInviteCodeCreatesRequestAndRunsImmediateJoinTransitions() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		WorkspaceJoinByInviteCodeRequest request = new WorkspaceJoinByInviteCodeRequest("A1B2-C3D4-E5F6");
		WorkspaceJoinRequestResultResponse joinedResponse = joinedResponse();

		when(transactionService.createInviteCodeRequest(currentUser, request)).thenReturn(100L);
		when(transactionService.transition(100L, WorkspaceJoinRequestEvent.JOIN)).thenReturn(joinedResponse);

		WorkspaceJoinRequestResultResponse response = service.joinByInviteCode(currentUser, request);

		assertEquals(joinedResponse, response);
		verify(transactionService).createInviteCodeRequest(currentUser, request);
		verify(transactionService).transition(100L, WorkspaceJoinRequestEvent.MARK_READY);
		verify(transactionService).transition(100L, WorkspaceJoinRequestEvent.APPROVE);
		verify(transactionService).transition(100L, WorkspaceJoinRequestEvent.JOIN);
		verify(failureRecorder, never()).record(
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void joinByInviteCodeRecordsFailedStateWhenTransitionFailsAfterRequestCreation() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		WorkspaceJoinByInviteCodeRequest request = new WorkspaceJoinByInviteCodeRequest("A1B2-C3D4-E5F6");
		IllegalStateException exception = new IllegalStateException("join failed");

		when(transactionService.createInviteCodeRequest(currentUser, request)).thenReturn(100L);
		doThrow(exception).when(transactionService).transition(100L, WorkspaceJoinRequestEvent.JOIN);

		IllegalStateException thrown = assertThrows(
			IllegalStateException.class,
			() -> service.joinByInviteCode(currentUser, request)
		);

		assertSame(exception, thrown);
		verify(failureRecorder).record(100L, exception);
	}

	@Test
	void joinByInviteCodeDoesNotRecordFailureWhenRequestWasNotCreated() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		WorkspaceJoinByInviteCodeRequest request = new WorkspaceJoinByInviteCodeRequest("A1B2-C3D4-E5F6");
		IllegalStateException exception = new IllegalStateException("invalid user");

		doThrow(exception).when(transactionService).createInviteCodeRequest(currentUser, request);

		assertThrows(IllegalStateException.class, () -> service.joinByInviteCode(currentUser, request));
		verify(failureRecorder, never()).record(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void requestsReturnsJoinRequestHistoriesForOwnerOrAdmin() {
		CurrentUser currentUser = new CurrentUser(1L, "owner@example.com", "owner");
		User owner = activeUser(1L);
		Workspace workspace = workspace(owner);
		WorkspaceMember ownerMembership = workspaceMember(100L, workspace, owner, WorkspaceMemberRole.OWNER);
		WorkspaceJoinRequest joinRequest = joinRequest(200L, workspace, activeUser(2L));
		joinRequest.recordRequested(joinRequest.getUser(), 1779888900L);
		joinRequest.markReady(joinRequest.getUser(), 1779888910L);

		when(currentUserProvider.findActive(currentUser)).thenReturn(owner);
		when(workspaceRepository.findActiveById(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(10L, 1L))
			.thenReturn(Optional.of(ownerMembership));
		when(workspaceJoinRequestRepository.findByWorkspaceIdWithHistories(10L)).thenReturn(List.of(joinRequest));

		WorkspaceJoinRequestsResponse response = service.requests(currentUser, 10L);

		assertEquals(1, response.joinRequests().size());
		assertEquals(200L, response.joinRequests().get(0).joinRequestId());
		assertEquals(WorkspaceJoinRequestStatus.READY, response.joinRequests().get(0).status());
		assertEquals(2, response.joinRequests().get(0).histories().size());
	}

	@Test
	void requestsRejectsMemberRequester() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		User member = activeUser(1L);
		Workspace workspace = workspace(member);
		WorkspaceMember membership = workspaceMember(100L, workspace, member, WorkspaceMemberRole.MEMBER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(member);
		when(workspaceRepository.findActiveById(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(10L, 1L))
			.thenReturn(Optional.of(membership));

		assertThrows(WorkspaceMemberAccessDeniedException.class, () -> service.requests(currentUser, 10L));
		verify(workspaceJoinRequestRepository, never()).findByWorkspaceIdWithHistories(10L);
	}

	@Test
	void requestsRejectsMissingWorkspace() {
		CurrentUser currentUser = new CurrentUser(1L, "owner@example.com", "owner");
		User owner = activeUser(1L);

		when(currentUserProvider.findActive(currentUser)).thenReturn(owner);
		when(workspaceRepository.findActiveById(10L)).thenReturn(Optional.empty());

		assertThrows(WorkspaceNotFoundException.class, () -> service.requests(currentUser, 10L));
		verify(workspaceMemberRepository, never()).findActiveByWorkspaceIdAndUserId(10L, 1L);
	}

	private WorkspaceJoinRequestResultResponse joinedResponse() {
		return new WorkspaceJoinRequestResultResponse(
			100L,
			10L,
			"Flowit",
			1L,
			"member",
			"member@example.com",
			300L,
			WorkspaceJoinRequestMethod.INVITE_CODE,
			"A1B2-C3D4-E5F6",
			WorkspaceJoinRequestStatus.JOINED,
			1779888930L
		);
	}

	private User activeUser(Long id) {
		return User.builder()
			.id(id)
			.email("user%s@example.com".formatted(id))
			.passwordHash("hash")
			.name("user%s".formatted(id))
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private Workspace workspace(User creator) {
		return Workspace.builder()
			.id(10L)
			.name("Flowit")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdBy(creator)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private WorkspaceJoinRequest joinRequest(Long id, Workspace workspace, User user) {
		return WorkspaceJoinRequest.builder()
			.id(id)
			.workspace(workspace)
			.user(user)
			.method(WorkspaceJoinRequestMethod.INVITE_CODE)
			.inviteCodeSnapshot("A1B2-C3D4-E5F6")
			.status(WorkspaceJoinRequestStatus.PENDING)
			.requestedAt(1779888900L)
			.createdAt(1779888900L)
			.updatedAt(1779888900L)
			.build();
	}

	private WorkspaceMember workspaceMember(
		Long id,
		Workspace workspace,
		User user,
		WorkspaceMemberRole role
	) {
		return WorkspaceMember.builder()
			.id(id)
			.workspace(workspace)
			.user(user)
			.role(role)
			.joinedAt(1L)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
