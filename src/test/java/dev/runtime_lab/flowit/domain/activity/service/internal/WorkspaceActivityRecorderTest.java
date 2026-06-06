package dev.runtime_lab.flowit.domain.activity.service.internal;

import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordAction;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordDomain;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityTargetType;
import dev.runtime_lab.flowit.domain.activity.entity.ActivityRecordSourceType;
import dev.runtime_lab.flowit.domain.activity.entity.WorkspaceActivityRecord;
import dev.runtime_lab.flowit.domain.activity.repository.WorkspaceActivityRecordRepository;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRemovalHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRoleHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberWithdrawalHistory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class WorkspaceActivityRecorderTest {

	private final WorkspaceActivityRecordRepository workspaceActivityRecordRepository =
		mock(WorkspaceActivityRecordRepository.class);
	private final WorkspaceActivityRecorder workspaceActivityRecorder = new WorkspaceActivityRecorder(
		workspaceActivityRecordRepository,
		JsonMapper.builder().build()
	);

	@Test
	void recordsMembershipLifecycleActionsOnly() {
		User actor = user(1L, "Actor");
		User target = user(2L, "Target");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor, WorkspaceMemberRole.OWNER);
		WorkspaceMember targetMember = workspaceMember(20L, workspace, target, WorkspaceMemberRole.MEMBER);
		ArgumentCaptor<WorkspaceActivityRecord> recordCaptor =
			ArgumentCaptor.forClass(WorkspaceActivityRecord.class);

		workspaceActivityRecorder.recordJoined(
			workspace,
			targetMember,
			target,
			WorkspaceJoinRequestHistory.builder().id(100L).build(),
			1780916400L
		);
		workspaceActivityRecorder.recordRoleChanged(
			workspace,
			actorMember,
			actor,
			targetMember,
			WorkspaceMemberRole.MEMBER,
			WorkspaceMemberRole.ADMIN,
			WorkspaceMemberRoleHistory.builder().id(101L).build(),
			1780916500L
		);
		workspaceActivityRecorder.recordRemoved(
			workspace,
			actorMember,
			actor,
			targetMember,
			WorkspaceMemberRole.ADMIN,
			WorkspaceMemberRemovalHistory.builder().id(102L).build(),
			1780916600L
		);
		workspaceActivityRecorder.recordWithdrawn(
			workspace,
			targetMember,
			target,
			WorkspaceMemberRole.MEMBER,
			null,
			WorkspaceMemberWithdrawalHistory.builder().id(103L).build(),
			1780916700L
		);

		verify(workspaceActivityRecordRepository, times(4)).save(recordCaptor.capture());
		List<WorkspaceActivityRecord> records = recordCaptor.getAllValues();

		assertEquals(ActivityRecordAction.JOINED, records.get(0).getAction());
		assertEquals(ActivityRecordSourceType.WORKSPACE_JOIN_REQUEST_HISTORY, records.get(0).getSourceType());
		assertEquals(ActivityRecordAction.ROLE_CHANGED, records.get(1).getAction());
		assertEquals(ActivityRecordSourceType.WORKSPACE_MEMBER_ROLE_HISTORY, records.get(1).getSourceType());
		assertEquals(ActivityRecordAction.REMOVED, records.get(2).getAction());
		assertEquals(ActivityRecordSourceType.WORKSPACE_MEMBER_REMOVAL_HISTORY, records.get(2).getSourceType());
		assertEquals(ActivityRecordAction.WITHDRAWN, records.get(3).getAction());
		assertEquals(ActivityRecordSourceType.WORKSPACE_MEMBER_WITHDRAWAL_HISTORY, records.get(3).getSourceType());

		for (WorkspaceActivityRecord record : records) {
			assertEquals(ActivityRecordDomain.WORKSPACE_MEMBER, record.getDomain());
			assertEquals(ActivityTargetType.WORKSPACE_MEMBER, record.getTargetType());
			assertEquals(20L, record.getTargetId());
			assertEquals("Target", record.getTargetDisplayNameSnapshot());
			assertTrue(record.getChangesJson().contains("\"element\""));
		}
	}

	private User user(Long id, String name) {
		return User.builder()
			.id(id)
			.email("user%s@example.com".formatted(id))
			.passwordHash("hash")
			.name(name)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private Workspace workspace(User creator) {
		return Workspace.builder()
			.id(1L)
			.name("Flowit")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdBy(creator)
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
