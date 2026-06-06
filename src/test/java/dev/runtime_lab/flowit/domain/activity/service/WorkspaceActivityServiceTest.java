package dev.runtime_lab.flowit.domain.activity.service;

import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordAction;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordDomain;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordListQuery;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordTopic;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityTargetType;
import dev.runtime_lab.flowit.domain.activity.entity.ActivityRecordSourceType;
import dev.runtime_lab.flowit.domain.activity.entity.WorkspaceActivityRecord;
import dev.runtime_lab.flowit.domain.activity.repository.WorkspaceActivityRecordRepository;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceAccessContext;
import dev.runtime_lab.flowit.domain.workspace.service.internal.WorkspaceAccessService;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkspaceActivityServiceTest {

	private final WorkspaceAccessService workspaceAccessService = mock(WorkspaceAccessService.class);
	private final WorkspaceActivityRecordRepository workspaceActivityRecordRepository =
		mock(WorkspaceActivityRecordRepository.class);
	private final Clock clock = Clock.fixed(Instant.ofEpochSecond(1780916400L), ZoneOffset.UTC);
	private final WorkspaceActivityService workspaceActivityService = new WorkspaceActivityService(
		workspaceAccessService,
		workspaceActivityRecordRepository,
		JsonMapper.builder().build(),
		clock
	);

	@Test
	void activityRecordsReturnsWorkspaceActivityRecords() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "Actor");
		User actor = user(1L, "actor@example.com", "Actor");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		WorkspaceActivityRecord activityRecord = WorkspaceActivityRecord.builder()
			.id(300L)
			.workspace(workspace)
			.sourceType(ActivityRecordSourceType.TASK_CHANGE_HISTORY)
			.sourceId(200L)
			.domain(ActivityRecordDomain.TASK)
			.action(ActivityRecordAction.STATUS_CHANGED)
			.actorWorkspaceMember(actorMember)
			.actorUser(actor)
			.actorDisplayNameSnapshot("Actor")
			.targetType(ActivityTargetType.TASK)
			.targetId(100L)
			.targetDisplayNameSnapshot("Login UI")
			.changesJson("""
				[{"element":"STATUS","from":"TO_DO","to":"IN_PROGRESS"}]
				""")
			.occurredAt(1780916400L)
			.build();

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(workspaceActivityRecordRepository.findByWorkspaceId(
			1L,
			ActivityRecordDomain.TASK,
			1780311600L
		)).thenReturn(List.of(activityRecord));

		var response = workspaceActivityService.activityRecords(
			currentUser,
			1L,
			new ActivityRecordListQuery(ActivityRecordTopic.TASK, 7)
		);

		assertEquals(1L, response.getTotalCount());
		assertEquals(1, response.getItems().size());
		var record = response.getItems().get(0);
		assertEquals(300L, record.id());
		assertEquals(ActivityRecordDomain.TASK, record.domain());
		assertEquals("Actor", record.actor().displayName());
		assertEquals(ActivityTargetType.TASK, record.target().type());
		assertEquals(100L, record.target().id());
		assertEquals("Login UI", record.target().displayName());
		assertEquals(ActivityRecordAction.STATUS_CHANGED, record.action());
		assertEquals(1, record.changeCount());
		assertEquals(List.of("STATUS"), record.changedElements().stream().map(Enum::name).toList());
		assertEquals("STATUS", record.changes().get(0).element().name());
	}

	@Test
	void activityRecordsDefaultsToAllTopicAndFiveDays() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "Actor");
		User actor = user(1L, "actor@example.com", "Actor");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(workspaceActivityRecordRepository.findByWorkspaceId(
			1L,
			null,
			1780484400L
		)).thenReturn(List.of());

		var response = workspaceActivityService.activityRecords(
			currentUser,
			1L,
			new ActivityRecordListQuery(null, null)
		);

		assertEquals(0L, response.getTotalCount());
		assertEquals(0, response.getItems().size());
	}

	private User user(Long id, String email, String name) {
		return User.builder()
			.id(id)
			.email(email)
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

	private WorkspaceMember workspaceMember(Long id, Workspace workspace, User user) {
		return WorkspaceMember.builder()
			.id(id)
			.workspace(workspace)
			.user(user)
			.role(WorkspaceMemberRole.MEMBER)
			.joinedAt(1L)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
