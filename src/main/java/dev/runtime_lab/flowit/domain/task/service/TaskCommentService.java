package dev.runtime_lab.flowit.domain.task.service;

import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentUpdateRequest;
import dev.runtime_lab.flowit.domain.task.entity.Task;
import dev.runtime_lab.flowit.domain.task.entity.TaskComment;
import dev.runtime_lab.flowit.domain.task.exception.TaskCommentAccessDeniedException;
import dev.runtime_lab.flowit.domain.task.exception.TaskCommentNotFoundException;
import dev.runtime_lab.flowit.domain.task.exception.TaskNotFoundException;
import dev.runtime_lab.flowit.domain.task.repository.TaskCommentRepository;
import dev.runtime_lab.flowit.domain.task.repository.TaskRepository;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.service.internal.WorkspaceAccessService;
import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceAccessContext;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.response.ApiListData;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.DEFAULT_COMMENT_PAGE_SIZE;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.MAX_COMMENT_PAGE_SIZE;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

	private final WorkspaceAccessService workspaceAccessService;
	private final TaskRepository taskRepository;
	private final TaskCommentRepository taskCommentRepository;
	private final Clock clock;

	@Transactional
	public TaskCommentCreateResponse create(
		CurrentUser currentUser,
		Long workspaceId,
		Long taskId,
		TaskCommentCreateRequest request
	) {
		WorkspaceAccessContext access = workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);
		Task task = findActiveTask(workspaceId, taskId);
		User authorUser = access.requester();
		WorkspaceMember authorMember = access.membership();
		long now = now();

		TaskComment comment = taskCommentRepository.save(TaskComment.builder()
			.workspace(access.workspace())
			.task(task)
			.authorWorkspaceMember(authorMember)
			.authorUser(authorUser)
			.authorDisplayNameSnapshot(authorUser.getName())
			.contentMarkdown(normalizeContent(request.contentMarkdown()))
			.createdAt(now)
			.updatedAt(now)
			.build());

		return TaskCommentCreateResponse.from(comment);
	}

	@Transactional(readOnly = true)
	public ApiListData<TaskCommentResponse> comments(
		CurrentUser currentUser,
		Long workspaceId,
		Long taskId,
		Integer page,
		Integer size
	) {
		WorkspaceAccessContext access = workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);
		findActiveTask(workspaceId, taskId);

		int pageValue = page(page);
		int sizeValue = size(size);

		List<TaskCommentResponse> responses = taskCommentRepository
			.findActiveByWorkspaceIdAndTaskId(workspaceId, taskId, pageValue, sizeValue)
			.stream()
			.map(comment -> TaskCommentResponse.from(comment, access.requester().getId()))
			.toList();

		long totalCount = taskCommentRepository.countActiveByWorkspaceIdAndTaskId(workspaceId, taskId);

		return ApiListData.of(responses, totalCount);
	}

	@Transactional
	public void update(
		CurrentUser currentUser,
		Long workspaceId,
		Long taskId,
		Long commentId,
		TaskCommentUpdateRequest request
	) {
		WorkspaceAccessContext access = workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);
		findActiveTask(workspaceId, taskId);
		TaskComment comment = findActiveCommentForUpdate(workspaceId, taskId, commentId);
		validateAuthor(comment, access.requester());

		String contentMarkdown = normalizeContent(request.contentMarkdown());
		if (Objects.equals(comment.getContentMarkdown(), contentMarkdown)) {
			return;
		}

		comment.updateContent(contentMarkdown, now());
	}

	@Transactional
	public void delete(CurrentUser currentUser, Long workspaceId, Long taskId, Long commentId) {
		WorkspaceAccessContext access = workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);
		findActiveTask(workspaceId, taskId);
		TaskComment comment = findActiveCommentForUpdate(workspaceId, taskId, commentId);
		validateAuthor(comment, access.requester());

		comment.softDelete(now());
	}

	private Task findActiveTask(Long workspaceId, Long taskId) {
		return taskRepository.findActiveByWorkspaceIdAndTaskId(workspaceId, taskId)
			.orElseThrow(TaskNotFoundException::new);
	}

	private TaskComment findActiveCommentForUpdate(Long workspaceId, Long taskId, Long commentId) {
		return taskCommentRepository
			.findActiveByWorkspaceIdAndTaskIdAndCommentIdForUpdate(workspaceId, taskId, commentId)
			.orElseThrow(TaskCommentNotFoundException::new);
	}

	private void validateAuthor(TaskComment comment, User requester) {
		if (!Objects.equals(comment.getAuthorUser().getId(), requester.getId())) {
			throw new TaskCommentAccessDeniedException();
		}
	}

	private String normalizeContent(String contentMarkdown) {
		return contentMarkdown.trim();
	}

	private long now() {
		return Instant.now(clock).getEpochSecond();
	}

	private int page(Integer page) {
		if (page == null || page < 0) {
			return 0;
		}

		return page;
	}

	private int size(Integer size) {
		if (size == null) {
			return DEFAULT_COMMENT_PAGE_SIZE;
		}
		if (size < 1) {
			return 1;
		}
		if (size > MAX_COMMENT_PAGE_SIZE) {
			return MAX_COMMENT_PAGE_SIZE;
		}

		return size;
	}
}
