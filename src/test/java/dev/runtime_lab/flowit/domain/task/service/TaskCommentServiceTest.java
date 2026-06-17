package dev.runtime_lab.flowit.domain.task.service;

import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentUpdateRequest;
import dev.runtime_lab.flowit.domain.task.entity.Task;
import dev.runtime_lab.flowit.domain.task.entity.TaskComment;
import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import dev.runtime_lab.flowit.domain.task.exception.TaskCommentAccessDeniedException;
import dev.runtime_lab.flowit.domain.task.repository.TaskCommentRepository;
import dev.runtime_lab.flowit.domain.task.repository.TaskRepository;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.service.internal.WorkspaceAccessService;
import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceAccessContext;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskCommentServiceTest {

	private final WorkspaceAccessService workspaceAccessService = mock(WorkspaceAccessService.class);
	private final TaskRepository taskRepository = mock(TaskRepository.class);
	private final TaskCommentRepository taskCommentRepository = mock(TaskCommentRepository.class);
	private final Clock clock = Clock.fixed(Instant.ofEpochSecond(1780916400L), ZoneOffset.UTC);
	private final TaskCommentService taskCommentService = new TaskCommentService(
		workspaceAccessService,
		taskRepository,
		taskCommentRepository,
		clock
	);

	@Test
	void createTrimsContentAndCreatesComment() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "Actor");
		User actor = user(1L, "actor@example.com", "Actor");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		Task task = task(100L, workspace, actor);
		ArgumentCaptor<TaskComment> commentCaptor = ArgumentCaptor.forClass(TaskComment.class);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(Optional.of(task));
		when(taskCommentRepository.save(any(TaskComment.class)))
			.thenReturn(taskComment(500L, workspace, task, actorMember, actor, "확인했습니다.", false));

		var response = taskCommentService.create(
			currentUser,
			1L,
			100L,
			new TaskCommentCreateRequest(" 확인했습니다. ")
		);

		assertEquals(500L, response.id());
		verify(taskCommentRepository).save(commentCaptor.capture());
		TaskComment savedComment = commentCaptor.getValue();
		assertEquals("확인했습니다.", savedComment.getContentMarkdown());
		assertEquals(actorMember, savedComment.getAuthorWorkspaceMember());
		assertEquals(actor, savedComment.getAuthorUser());
		assertEquals("Actor", savedComment.getAuthorDisplayNameSnapshot());
		assertFalse(savedComment.isEdited());
		assertEquals(1780916400L, savedComment.getCreatedAt());
	}

	@Test
	void commentsReturnsOldestFirstPageFromRepository() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "Actor");
		User actor = user(1L, "actor@example.com", "Actor");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		Task task = task(100L, workspace, actor);
		TaskComment comment = taskComment(500L, workspace, task, actorMember, actor, "수정된 댓글입니다.", true);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(Optional.of(task));
		when(taskCommentRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L, 0, 20))
			.thenReturn(List.of(comment));
		when(taskCommentRepository.countActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(1L);

		var response = taskCommentService.comments(currentUser, 1L, 100L, null, null);

		assertEquals(1L, response.getTotalCount());
		assertEquals(500L, response.getItems().get(0).id());
		assertTrue(response.getItems().get(0).edited());
		assertTrue(response.getItems().get(0).editable());
		assertTrue(response.getItems().get(0).ownedByRequester());
		verify(taskCommentRepository).findActiveByWorkspaceIdAndTaskId(1L, 100L, 0, 20);
	}

	@Test
	void commentsMarksNonAuthorCommentAsNotEditable() {
		CurrentUser currentUser = new CurrentUser(2L, "other@example.com", "Other");
		User author = user(1L, "actor@example.com", "Actor");
		User requester = user(2L, "other@example.com", "Other");
		Workspace workspace = workspace(author);
		WorkspaceMember authorMember = workspaceMember(10L, workspace, author);
		WorkspaceMember requesterMember = workspaceMember(11L, workspace, requester);
		Task task = task(100L, workspace, author);
		TaskComment comment = taskComment(500L, workspace, task, authorMember, author, "댓글입니다.", false);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(requester, workspace, requesterMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(Optional.of(task));
		when(taskCommentRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L, 0, 20))
			.thenReturn(List.of(comment));
		when(taskCommentRepository.countActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(1L);

		var response = taskCommentService.comments(currentUser, 1L, 100L, null, null);

		assertFalse(response.getItems().get(0).editable());
		assertFalse(response.getItems().get(0).ownedByRequester());
	}

	@Test
	void updateMarksCommentAsEditedWhenAuthorUpdatesContent() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "Actor");
		User actor = user(1L, "actor@example.com", "Actor");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		Task task = task(100L, workspace, actor);
		TaskComment comment = taskComment(500L, workspace, task, actorMember, actor, "이전 댓글입니다.", false);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(Optional.of(task));
		when(taskCommentRepository.findActiveByWorkspaceIdAndTaskIdAndCommentIdForUpdate(1L, 100L, 500L))
			.thenReturn(Optional.of(comment));

		taskCommentService.update(
			currentUser,
			1L,
			100L,
			500L,
			new TaskCommentUpdateRequest(" 수정된 댓글입니다. ")
		);

		assertEquals("수정된 댓글입니다.", comment.getContentMarkdown());
		assertTrue(comment.isEdited());
		assertEquals(1780916400L, comment.getUpdatedAt());
	}

	@Test
	void updateDoesNotMarkEditedWhenContentIsSame() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "Actor");
		User actor = user(1L, "actor@example.com", "Actor");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		Task task = task(100L, workspace, actor);
		TaskComment comment = taskComment(500L, workspace, task, actorMember, actor, "같은 댓글입니다.", false);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(Optional.of(task));
		when(taskCommentRepository.findActiveByWorkspaceIdAndTaskIdAndCommentIdForUpdate(1L, 100L, 500L))
			.thenReturn(Optional.of(comment));

		taskCommentService.update(
			currentUser,
			1L,
			100L,
			500L,
			new TaskCommentUpdateRequest(" 같은 댓글입니다. ")
		);

		assertFalse(comment.isEdited());
		assertEquals(1L, comment.getUpdatedAt());
	}

	@Test
	void updateRejectsNonAuthor() {
		CurrentUser currentUser = new CurrentUser(2L, "other@example.com", "Other");
		User actor = user(1L, "actor@example.com", "Actor");
		User other = user(2L, "other@example.com", "Other");
		Workspace workspace = workspace(actor);
		WorkspaceMember authorMember = workspaceMember(10L, workspace, actor);
		WorkspaceMember otherMember = workspaceMember(11L, workspace, other);
		Task task = task(100L, workspace, actor);
		TaskComment comment = taskComment(500L, workspace, task, authorMember, actor, "댓글입니다.", false);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(other, workspace, otherMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(Optional.of(task));
		when(taskCommentRepository.findActiveByWorkspaceIdAndTaskIdAndCommentIdForUpdate(1L, 100L, 500L))
			.thenReturn(Optional.of(comment));

		assertThrows(TaskCommentAccessDeniedException.class, () -> taskCommentService.update(
			currentUser,
			1L,
			100L,
			500L,
			new TaskCommentUpdateRequest("수정 시도입니다.")
		));
	}

	@Test
	void updateAllowsSameUserWithDifferentMembership() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "Actor");
		User actor = user(1L, "actor@example.com", "Actor");
		Workspace workspace = workspace(actor);
		WorkspaceMember originalMember = workspaceMember(10L, workspace, actor);
		WorkspaceMember rejoinedMember = workspaceMember(11L, workspace, actor);
		Task task = task(100L, workspace, actor);
		TaskComment comment = taskComment(500L, workspace, task, originalMember, actor, "이전 댓글입니다.", false);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, rejoinedMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(Optional.of(task));
		when(taskCommentRepository.findActiveByWorkspaceIdAndTaskIdAndCommentIdForUpdate(1L, 100L, 500L))
			.thenReturn(Optional.of(comment));

		taskCommentService.update(
			currentUser,
			1L,
			100L,
			500L,
			new TaskCommentUpdateRequest("다시 수정한 댓글입니다.")
		);

		assertEquals("다시 수정한 댓글입니다.", comment.getContentMarkdown());
		assertTrue(comment.isEdited());
	}

	@Test
	void deleteSoftDeletesCommentWhenAuthorDeletes() {
		CurrentUser currentUser = new CurrentUser(1L, "actor@example.com", "Actor");
		User actor = user(1L, "actor@example.com", "Actor");
		Workspace workspace = workspace(actor);
		WorkspaceMember actorMember = workspaceMember(10L, workspace, actor);
		Task task = task(100L, workspace, actor);
		TaskComment comment = taskComment(500L, workspace, task, actorMember, actor, "댓글입니다.", false);

		when(workspaceAccessService.resolveMemberAccess(currentUser, 1L))
			.thenReturn(new WorkspaceAccessContext(actor, workspace, actorMember));
		when(taskRepository.findActiveByWorkspaceIdAndTaskId(1L, 100L)).thenReturn(Optional.of(task));
		when(taskCommentRepository.findActiveByWorkspaceIdAndTaskIdAndCommentIdForUpdate(1L, 100L, 500L))
			.thenReturn(Optional.of(comment));

		taskCommentService.delete(currentUser, 1L, 100L, 500L);

		assertEquals(1780916400L, comment.getDeletedAt());
		assertEquals(1780916400L, comment.getUpdatedAt());
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

	private Task task(Long id, Workspace workspace, User creator) {
		return Task.builder()
			.id(id)
			.workspace(workspace)
			.title("Login UI")
			.descriptionMarkdown("### Login screen")
			.status(TaskStatus.TODO)
			.priority(TaskPriority.HIGH)
			.progress(0)
			.createdBy(creator)
			.createdAt(1L)
			.updatedAt(1L)
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
