package dev.runtime_lab.flowit.domain.task.service;

import dev.runtime_lab.flowit.domain.activity.service.internal.WorkspaceActivityRecorder;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskProgressUpdateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskStatusUpdateRequest;
import dev.runtime_lab.flowit.domain.task.entity.Task;
import dev.runtime_lab.flowit.domain.task.entity.TaskChangeHistory;
import dev.runtime_lab.flowit.domain.task.entity.TaskComment;
import dev.runtime_lab.flowit.domain.task.entity.TaskHistoryAction;
import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import dev.runtime_lab.flowit.domain.task.entity.TaskTag;
import dev.runtime_lab.flowit.domain.task.repository.TaskChangeHistoryRepository;
import dev.runtime_lab.flowit.domain.task.repository.TaskCommentRepository;
import dev.runtime_lab.flowit.domain.task.repository.TaskRepository;
import dev.runtime_lab.flowit.domain.task.repository.TaskTagRepository;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import tools.jackson.databind.json.JsonMapper;

class TaskServiceTest {

	private static final Long START_DATE = 1780876800L;
	private static final Long DUE_DATE = 1781222400L;

	private final WorkspaceAccessService workspaceAccessService = mock(WorkspaceAccessService.class);
	private final TaskRepository taskRepository = mock(TaskRepository.class);
	private final TaskTagRepository taskTagRepository = mock(TaskTagRepository.class);
	private final TaskChangeHistoryRepository taskChangeHistoryRepository = mock(TaskChangeHistoryRepository.class);
	private final TaskCommentRepository taskCommentRepository = mock(TaskCommentRepository.class);
	private final WorkspaceActivityRecorder workspaceActivityRecorder = mock(WorkspaceActivityRecorder.class);
	private final Clock clock = Clock.fixed(Instant.ofEpochSecond(1780916400L), ZoneOffset.UTC);
	private final TaskService taskService = new TaskService(
		workspaceAccessService,
		taskRepository,
		taskTagRepository,
		taskChangeHistoryRepository,
		taskCommentRepository,
		workspaceActivityRecorder,
		JsonMapper.builder().build(),
		clock
	);

	@Test
	void createNormalizesTagsAndRecordsCreatedHistory() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "김철수");
		User actor = user(1L, "actor@example.com", "김철수");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		WorkspaceMember assignee = workspaceMember(12L, workspace, user(2L, "assignee@example.com", "홍길동"));
		Task savedTask = task(100L, workspace, assignee, actor, 0);
		TaskCreateRequest request = new TaskCreateRequest(
			" 로그인 UI 구현 ",
			"### 로그인 화면",
			TaskStatus.TODO,
			12L,
			TaskPriority.HIGH,
			START_DATE,
			DUE_DATE,
			List.of("Frontend", "frontend", " ui ")
		);
		ArgumentCaptor<TaskTag> tagCaptor = ArgumentCaptor.forClass(TaskTag.class);
		ArgumentCaptor<TaskChangeHistory> historyCaptor = ArgumentCaptor.forClass(TaskChangeHistory.class);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(workspaceAccessService.findActiveMember(1L, 12L)).thenReturn(Optional.of(assignee));
		when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

		var response = taskService.create(currentUser, 1L, request);

		assertEquals(100L, response.id());
		assertEquals(1780916300L, response.createdAt());
		verify(taskTagRepository, times(2)).save(tagCaptor.capture());
		assertEquals(List.of("Frontend", "ui"), tagCaptor.getAllValues().stream().map(TaskTag::getName).toList());
		assertEquals(List.of("frontend", "ui"), tagCaptor.getAllValues().stream().map(TaskTag::getNormalizedName).toList());

		verify(taskChangeHistoryRepository).save(historyCaptor.capture());
		TaskChangeHistory history = historyCaptor.getValue();
		assertEquals(TaskHistoryAction.CREATED, history.getAction());
		assertEquals("로그인 UI 구현", history.getTaskTitleSnapshot());
		assertEquals("김철수", history.getActorDisplayNameSnapshot());
		assertTrue(history.getChangesJson().contains("\"element\":\"TITLE\""));
		assertTrue(history.getChangesJson().contains("\"element\":\"TAGS\""));
	}

	@Test
	void createAllowsUnassignedTask() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "김철수");
		User actor = user(1L, "actor@example.com", "김철수");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		Task savedTask = task(100L, workspace, null, actor, 0);
		TaskCreateRequest request = new TaskCreateRequest(
			"로그인 UI 구현",
			"### 로그인 화면",
			TaskStatus.TODO,
			null,
			TaskPriority.HIGH,
			START_DATE,
			DUE_DATE,
			List.of()
		);
		ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
		ArgumentCaptor<TaskChangeHistory> historyCaptor = ArgumentCaptor.forClass(TaskChangeHistory.class);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.save(taskCaptor.capture())).thenReturn(savedTask);

		taskService.create(currentUser, 1L, request);

		assertNull(taskCaptor.getValue().getAssignee());
		verify(workspaceAccessService, never()).findActiveMember(1L, null);
		verify(taskChangeHistoryRepository).save(historyCaptor.capture());
		assertTrue(!historyCaptor.getValue().getChangesJson().contains("\"element\":\"ASSIGNEE\""));
	}

	@Test
	void updateCanRemoveAssignee() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "김철수");
		User actor = user(1L, "actor@example.com", "김철수");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		WorkspaceMember assignee = workspaceMember(12L, workspace, user(2L, "assignee@example.com", "홍길동"));
		Task task = task(100L, workspace, assignee, actor, 10);
		var request = new dev.runtime_lab.flowit.domain.task.dto.TaskUpdateRequest(
			"로그인 UI 구현",
			"### 로그인 화면",
			TaskStatus.TODO,
			null,
			TaskPriority.HIGH,
			START_DATE,
			DUE_DATE,
			List.of()
		);
		ArgumentCaptor<TaskChangeHistory> historyCaptor = ArgumentCaptor.forClass(TaskChangeHistory.class);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskIdForUpdate(1L, 100L)).thenReturn(Optional.of(task));
		when(taskTagRepository.findByTaskId(100L)).thenReturn(List.of());

		taskService.update(currentUser, 1L, 100L, request);

		assertNull(task.getAssignee());
		verify(workspaceAccessService, never()).findActiveMember(1L, null);
		verify(taskChangeHistoryRepository).save(historyCaptor.capture());
		assertTrue(historyCaptor.getValue().getChangesJson().contains("\"element\":\"ASSIGNEE\""));
		assertTrue(historyCaptor.getValue().getChangesJson().contains("\"to\":null"));
	}

	@Test
	void updateProgressRecordsProgressChangedHistory() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "김철수");
		User actor = user(1L, "actor@example.com", "김철수");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		WorkspaceMember assignee = workspaceMember(12L, workspace, user(2L, "assignee@example.com", "홍길동"));
		Task task = task(100L, workspace, assignee, actor, 10);
		ArgumentCaptor<TaskChangeHistory> historyCaptor = ArgumentCaptor.forClass(TaskChangeHistory.class);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskIdForUpdate(1L, 100L)).thenReturn(Optional.of(task));

		taskService.updateProgress(currentUser, 1L, 100L, new TaskProgressUpdateRequest(65));

		assertEquals(65, task.getProgress());
		assertEquals(1780916400L, task.getUpdatedAt());
		verify(taskChangeHistoryRepository).save(historyCaptor.capture());
		TaskChangeHistory history = historyCaptor.getValue();
		assertEquals(TaskHistoryAction.PROGRESS_CHANGED, history.getAction());
		assertTrue(history.getChangesJson().contains("\"element\":\"PROGRESS\""));
		assertTrue(history.getChangesJson().contains("\"from\":10"));
		assertTrue(history.getChangesJson().contains("\"to\":65"));
	}

	@Test
	void updateStatusRecordsStatusChangedHistory() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "김철수");
		User actor = user(1L, "actor@example.com", "김철수");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		WorkspaceMember assignee = workspaceMember(12L, workspace, user(2L, "assignee@example.com", "홍길동"));
		Task task = task(100L, workspace, assignee, actor, 10);
		ArgumentCaptor<TaskChangeHistory> historyCaptor = ArgumentCaptor.forClass(TaskChangeHistory.class);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskIdForUpdate(1L, 100L)).thenReturn(Optional.of(task));

		taskService.updateStatus(currentUser, 1L, 100L, new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS));

		assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
		assertEquals(1780916400L, task.getUpdatedAt());
		verify(taskChangeHistoryRepository).save(historyCaptor.capture());
		TaskChangeHistory history = historyCaptor.getValue();
		assertEquals(TaskHistoryAction.STATUS_CHANGED, history.getAction());
		assertTrue(history.getChangesJson().contains("\"element\":\"STATUS\""));
		assertTrue(history.getChangesJson().contains("\"from\":\"TODO\""));
		assertTrue(history.getChangesJson().contains("\"to\":\"IN_PROGRESS\""));
	}

	@Test
	void updateStatusSkipsWhenStatusUnchanged() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "김철수");
		User actor = user(1L, "actor@example.com", "김철수");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		WorkspaceMember assignee = workspaceMember(12L, workspace, user(2L, "assignee@example.com", "홍길동"));
		Task task = task(100L, workspace, assignee, actor, 10);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskIdForUpdate(1L, 100L)).thenReturn(Optional.of(task));

		taskService.updateStatus(currentUser, 1L, 100L, new TaskStatusUpdateRequest(TaskStatus.TODO));

		assertEquals(TaskStatus.TODO, task.getStatus());
		assertEquals(1780916300L, task.getUpdatedAt());
		verify(taskChangeHistoryRepository, never()).save(any(TaskChangeHistory.class));
		verify(workspaceActivityRecorder, never()).recordTask(any(TaskChangeHistory.class), any());
	}

	@Test
	void getIncludesFirstTaskCommentPage() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "Actor");
		User actor = user(1L, "actor@example.com", "Actor");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		Task task = task(100L, workspace, null, actor, 0);
		TaskComment comment = taskComment(500L, workspace, task, actorMember, actor, "확인했습니다.", false);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(Optional.of(task));
		when(taskTagRepository.findByTaskId(100L)).thenReturn(List.of());
		when(taskCommentRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L, 0, 20))
			.thenReturn(List.of(comment));
		when(taskCommentRepository.countActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(1L);

		var response = taskService.get(currentUser, 1L, 100L);

		assertEquals(1L, response.commentPage().getTotalCount());
		assertEquals(500L, response.commentPage().getItems().get(0).id());
		assertTrue(response.commentPage().getItems().get(0).editable());
		assertTrue(response.commentPage().getItems().get(0).ownedByRequester());
		verify(taskCommentRepository).findActiveByWorkspaceIdAndTaskId(1L, 100L, 0, 20);
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

	private Task task(Long id, Workspace workspace, WorkspaceMember assignee, User creator, Integer progress) {
		return Task.builder()
			.id(id)
			.workspace(workspace)
			.title("로그인 UI 구현")
			.descriptionMarkdown("### 로그인 화면")
			.status(TaskStatus.TODO)
			.priority(TaskPriority.HIGH)
			.assignee(assignee)
			.startDate(START_DATE)
			.dueDate(DUE_DATE)
			.progress(progress)
			.createdBy(creator)
			.createdAt(1780916300L)
			.updatedAt(1780916300L)
			.build();
	}

	private TaskComment taskComment(
		Long id,
		Workspace workspace,
		Task task,
		WorkspaceMember authorMember,
		User authorUser,
		String contentMarkdown,
		boolean edited
	) {
		return TaskComment.builder()
			.id(id)
			.workspace(workspace)
			.task(task)
			.authorWorkspaceMember(authorMember)
			.authorUser(authorUser)
			.authorDisplayNameSnapshot(authorUser.getName())
			.contentMarkdown(contentMarkdown)
			.edited(edited)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
