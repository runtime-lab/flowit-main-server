package dev.runtime_lab.flowit.domain.workspace.statemachine;

import dev.runtime_lab.flowit.domain.activity.service.internal.WorkspaceActivityRecorder;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestMethod;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.exception.DuplicateWorkspaceMemberException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceJoinRequestHistoryRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {
	WorkspaceJoinStateMachineConfig.class,
	WorkspaceJoinStateMachineService.class,
	WorkspaceJoinReadyGuard.class,
	WorkspaceJoinReadyAction.class,
	WorkspaceJoinApproveAction.class,
	WorkspaceJoinAction.class,
	WorkspaceJoinFailAction.class,
	WorkspaceJoinStateMachineServiceTest.TestConfig.class
})
class WorkspaceJoinStateMachineServiceTest {

	@jakarta.annotation.Resource
	private WorkspaceJoinStateMachineService service;

	@jakarta.annotation.Resource
	private WorkspaceMemberRepository workspaceMemberRepository;

	@jakarta.annotation.Resource
	private WorkspaceJoinRequestHistoryRepository historyRepository;

	@jakarta.annotation.Resource
	private WorkspaceActivityRecorder workspaceActivityRecorder;

	@BeforeEach
	void setUp() {
		reset(workspaceMemberRepository, historyRepository, workspaceActivityRecorder);
	}

	@Test
	void sendRunsReadyApproveJoinTransitionsThroughSpringStateMachine() {
		User requester = activeUser(1L);
		Workspace workspace = workspace(activeUser(2L));
		WorkspaceJoinRequest joinRequest = joinRequest(100L, workspace, requester);
		WorkspaceMember savedMember = workspaceMember(300L, workspace, requester, WorkspaceMemberRole.MEMBER);

		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.empty());
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(savedMember);

		service.send(joinRequest, WorkspaceJoinRequestEvent.MARK_READY, requester);
		service.send(joinRequest, WorkspaceJoinRequestEvent.APPROVE, requester);
		service.send(joinRequest, WorkspaceJoinRequestEvent.JOIN, requester);

		assertEquals(WorkspaceJoinRequestStatus.JOINED, joinRequest.getStatus());
		assertEquals(1779889000L, joinRequest.getReadyAt());
		assertEquals(1779889000L, joinRequest.getApprovedAt());
		assertEquals(1779889000L, joinRequest.getJoinedAt());
		assertEquals(savedMember, joinRequest.getWorkspaceMember());
		assertEquals(3, joinRequest.getHistories().size());
		verify(historyRepository).save(joinRequest.getHistories().get(0));
		verify(historyRepository).save(joinRequest.getHistories().get(1));
		verify(historyRepository).save(joinRequest.getHistories().get(2));
		verify(workspaceActivityRecorder).recordJoined(
			workspace,
			savedMember,
			requester,
			joinRequest.getHistories().get(2),
			1779889000L
		);
	}

	@Test
	void sendRejectsReadyTransitionWhenUserAlreadyJoinedWorkspace() {
		User requester = activeUser(1L);
		Workspace workspace = workspace(activeUser(2L));
		WorkspaceJoinRequest joinRequest = joinRequest(100L, workspace, requester);
		WorkspaceMember existingMember = workspaceMember(300L, workspace, requester, WorkspaceMemberRole.MEMBER);

		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(existingMember));

		assertThrows(DuplicateWorkspaceMemberException.class,
			() -> service.send(joinRequest, WorkspaceJoinRequestEvent.MARK_READY, requester));
		assertEquals(WorkspaceJoinRequestStatus.PENDING, joinRequest.getStatus());
	}

	@Test
	void sendFailureTransitionsCurrentRequestToFailed() {
		User requester = activeUser(1L);
		Workspace workspace = workspace(activeUser(2L));
		WorkspaceJoinRequest joinRequest = joinRequest(100L, workspace, requester);
		joinRequest.markReady(requester, 1779888990L);

		service.sendFailure(joinRequest, requester, "INTERNAL_ERROR", "membership creation failed");

		assertEquals(WorkspaceJoinRequestStatus.FAILED, joinRequest.getStatus());
		assertEquals(1779889000L, joinRequest.getFailedAt());
		assertEquals("INTERNAL_ERROR", joinRequest.getFailureCode());
		assertEquals("membership creation failed", joinRequest.getFailureMessage());
		assertEquals(2, joinRequest.getHistories().size());
		assertEquals(WorkspaceJoinRequestEvent.FAIL, joinRequest.getHistories().get(1).getEvent());
		verify(historyRepository).save(joinRequest.getHistories().get(1));
	}

	@Configuration
	static class TestConfig {

		@Bean
		WorkspaceMemberRepository workspaceMemberRepository() {
			return mock(WorkspaceMemberRepository.class);
		}

		@Bean
		WorkspaceJoinRequestHistoryRepository workspaceJoinRequestHistoryRepository() {
			return mock(WorkspaceJoinRequestHistoryRepository.class);
		}

		@Bean
		WorkspaceActivityRecorder workspaceActivityRecorder() {
			return mock(WorkspaceActivityRecorder.class);
		}

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.ofEpochSecond(1779889000L), ZoneOffset.UTC);
		}
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
			.requestedAt(1L)
			.createdAt(1L)
			.updatedAt(1L)
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
