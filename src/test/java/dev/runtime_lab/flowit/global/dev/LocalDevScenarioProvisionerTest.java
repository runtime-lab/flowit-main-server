package dev.runtime_lab.flowit.global.dev;

import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentUpdateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskProgressUpdateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskUpdateRequest;
import dev.runtime_lab.flowit.domain.task.entity.Task;
import dev.runtime_lab.flowit.domain.task.entity.TaskComment;
import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import dev.runtime_lab.flowit.domain.task.repository.TaskCommentRepository;
import dev.runtime_lab.flowit.domain.task.repository.TaskRepository;
import dev.runtime_lab.flowit.domain.task.service.TaskCommentService;
import dev.runtime_lab.flowit.domain.task.service.TaskService;
import dev.runtime_lab.flowit.domain.user.dto.JoinRequest;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.domain.user.service.UserJoinService;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceService;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class LocalDevScenarioProvisionerTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneOffset.UTC);

	private final UserJoinService userJoinService = mock(UserJoinService.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final WorkspaceService workspaceService = mock(WorkspaceService.class);
	private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
	private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
	private final TaskService taskService = mock(TaskService.class);
	private final TaskRepository taskRepository = mock(TaskRepository.class);
	private final TaskCommentService taskCommentService = mock(TaskCommentService.class);
	private final TaskCommentRepository taskCommentRepository = mock(TaskCommentRepository.class);
	private final LocalDevSampleRecordRegistry sampleRecordRegistry = mock(LocalDevSampleRecordRegistry.class);
	private final LocalDevScenarioProvisioner provisioner = new LocalDevScenarioProvisioner(
		new LocalDevBaseScenarioProvisioner(
			userJoinService,
			userRepository,
			workspaceService,
			workspaceRepository,
			workspaceMemberRepository,
			sampleRecordRegistry,
			CLOCK
		),
		new LocalDevTaskScenarioStep(taskService, taskRepository, CLOCK, sampleRecordRegistry),
		new LocalDevTaskCommentScenarioStep(taskCommentService, taskCommentRepository, sampleRecordRegistry),
		sampleRecordRegistry
	);

	@Test
	void createsMissingSampleScenarioFromWorkspaceSupplyChain() {
		User owner = owner();
		Workspace workspace = workspace(owner);
		WorkspaceMember membership = ownerMembership(workspace, owner);

		when(sampleRecordRegistry.findEntityId(anyString(), any(LocalDevSampleRecordType.class)))
			.thenReturn(Optional.empty());
		when(userRepository.findActiveByEmail(LocalDevScenarioSamples.LOCAL_OWNER_EMAIL))
			.thenReturn(Optional.<User>empty(), Optional.of(owner));
		when(workspaceRepository.findActiveByCreatedByUserIdAndName(
			owner.getId(),
			LocalDevScenarioSamples.LOCAL_WORKSPACE_NAME
		)).thenReturn(Optional.<Workspace>empty());
		when(workspaceService.create(
			any(CurrentUser.class),
			any(WorkspaceCreateRequest.class)
		)).thenReturn(new WorkspaceCreateResponse(workspace.getId(), 1L));
		when(workspaceRepository.findActiveById(workspace.getId())).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspace.getId(), owner.getId()))
			.thenReturn(Optional.of(membership));
		when(taskRepository.findActiveByWorkspaceIdAndTitle(eq(workspace.getId()), anyString()))
			.thenReturn(Optional.<Task>empty());
		when(taskCommentRepository.findActiveByTaskIdAndContentMarkdown(any(), anyString()))
			.thenReturn(Optional.<TaskComment>empty());
		when(taskService.create(
			any(CurrentUser.class),
			eq(workspace.getId()),
			any(TaskCreateRequest.class)
		)).thenReturn(
			new TaskCreateResponse(1000L, 1L),
			new TaskCreateResponse(1001L, 1L),
			new TaskCreateResponse(1002L, 1L)
		);
		when(taskCommentService.create(
			any(CurrentUser.class),
			eq(workspace.getId()),
			any(),
			any(TaskCommentCreateRequest.class)
		)).thenReturn(
			new TaskCommentCreateResponse(2000L, 1L),
			new TaskCommentCreateResponse(2001L, 1L)
		);

		provisioner.provision();

		verify(sampleRecordRegistry).ensureReady();
		verify(userJoinService).join(any(JoinRequest.class));
		verify(workspaceService).create(any(CurrentUser.class), any(WorkspaceCreateRequest.class));
		verify(taskService, times(3)).create(
			any(CurrentUser.class),
			eq(workspace.getId()),
			any(TaskCreateRequest.class)
		);
		verify(taskService, never()).updateProgress(
			any(CurrentUser.class),
			eq(workspace.getId()),
			any(),
			any(TaskProgressUpdateRequest.class)
		);
		verify(taskCommentService, times(2)).create(
			any(CurrentUser.class),
			eq(workspace.getId()),
			any(),
			any(TaskCommentCreateRequest.class)
		);
		verify(sampleRecordRegistry, times(6)).upsert(
			anyString(),
			any(LocalDevSampleRecordType.class),
			any()
		);
	}

	@Test
	void reconcilesExistingSampleScenarioDataWithoutDuplicateCreation() {
		User owner = owner();
		Workspace workspace = workspace(owner);
		WorkspaceMember membership = ownerMembership(workspace, owner);
		Task existingTask = task(workspace, owner);
		TaskComment existingComment = taskComment(workspace, existingTask, membership, owner);

		when(sampleRecordRegistry.findEntityId(anyString(), any(LocalDevSampleRecordType.class)))
			.thenReturn(Optional.empty());
		when(userRepository.findActiveByEmail(LocalDevScenarioSamples.LOCAL_OWNER_EMAIL))
			.thenReturn(Optional.of(owner));
		when(workspaceRepository.findActiveByCreatedByUserIdAndName(
			owner.getId(),
			LocalDevScenarioSamples.LOCAL_WORKSPACE_NAME
		)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspace.getId(), owner.getId()))
			.thenReturn(Optional.of(membership));
		when(taskRepository.findActiveByWorkspaceIdAndTitle(eq(workspace.getId()), anyString()))
			.thenReturn(Optional.of(existingTask));
		when(taskCommentRepository.findActiveByTaskIdAndContentMarkdown(any(), anyString()))
			.thenReturn(Optional.of(existingComment));

		provisioner.provision();

		verify(sampleRecordRegistry).ensureReady();
		verify(userJoinService, never()).join(any(JoinRequest.class));
		verify(workspaceService, never()).create(any(CurrentUser.class), any(WorkspaceCreateRequest.class));
		verify(taskService, never()).create(
			any(CurrentUser.class),
			eq(workspace.getId()),
			any(TaskCreateRequest.class)
		);
		verify(taskService, times(3)).update(
			any(CurrentUser.class),
			eq(workspace.getId()),
			eq(existingTask.getId()),
			any(TaskUpdateRequest.class)
		);
		verify(taskService, times(2)).updateProgress(
			any(CurrentUser.class),
			eq(workspace.getId()),
			eq(existingTask.getId()),
			any(TaskProgressUpdateRequest.class)
		);
		verify(taskCommentService, never()).create(
			any(CurrentUser.class),
			eq(workspace.getId()),
			any(),
			any(TaskCommentCreateRequest.class)
		);
		verify(taskCommentService, never()).update(
			any(CurrentUser.class),
			eq(workspace.getId()),
			any(),
			any(),
			any(TaskCommentUpdateRequest.class)
		);
	}

	private User owner() {
		return User.builder()
			.id(1L)
			.email(LocalDevScenarioSamples.LOCAL_OWNER_EMAIL)
			.passwordHash("hash")
			.name(LocalDevScenarioSamples.LOCAL_OWNER_NICKNAME)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private Workspace workspace(User owner) {
		return Workspace.builder()
			.id(10L)
			.name(LocalDevScenarioSamples.LOCAL_WORKSPACE_NAME)
			.description("description")
			.inviteCode("ABCD-EFGH-IJKL")
			.createdBy(owner)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private WorkspaceMember ownerMembership(Workspace workspace, User owner) {
		return WorkspaceMember.builder()
			.id(100L)
			.workspace(workspace)
			.user(owner)
			.role(WorkspaceMemberRole.OWNER)
			.joinedAt(1L)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private Task task(Workspace workspace, User owner) {
		return Task.builder()
			.id(1000L)
			.workspace(workspace)
			.title("sample")
			.status(TaskStatus.TODO)
			.priority(TaskPriority.MEDIUM)
			.createdBy(owner)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private TaskComment taskComment(
		Workspace workspace,
		Task task,
		WorkspaceMember membership,
		User owner
	) {
		return TaskComment.builder()
			.id(2000L)
			.workspace(workspace)
			.task(task)
			.authorWorkspaceMember(membership)
			.authorUser(owner)
			.authorDisplayNameSnapshot(owner.getName())
			.contentMarkdown(LocalDevScenarioSamples.TASK_COMMENTS.get(0).contentMarkdown())
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
