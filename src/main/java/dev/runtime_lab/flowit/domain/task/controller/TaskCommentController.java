package dev.runtime_lab.flowit.domain.task.controller;

import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentUpdateRequest;
import dev.runtime_lab.flowit.domain.task.service.TaskCommentService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUser;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.response.ApiCreatedData;
import dev.runtime_lab.flowit.global.web.response.ApiEmptyData;
import dev.runtime_lab.flowit.global.web.response.ApiListData;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/workspaces/{workspaceId}/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

	private final TaskCommentService taskCommentService;

	@PostMapping
	public ResponseEntity<ApiCreatedData> create(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@PathVariable Long taskId,
		@Valid @RequestBody TaskCommentCreateRequest request
	) {
		TaskCommentCreateResponse response = taskCommentService.create(currentUser, workspaceId, taskId, request);

		return ResponseEntity
			.created(URI.create(
				"/v1/workspaces/%d/tasks/%d/comments/%d".formatted(workspaceId, taskId, response.id())
			))
			.body(ApiCreatedData.afterCreated(response.id()));
	}

	@GetMapping
	public ApiListData<TaskCommentResponse> comments(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@PathVariable Long taskId,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size
	) {
		return taskCommentService.comments(currentUser, workspaceId, taskId, page, size);
	}

	@PatchMapping("/{commentId}")
	public ApiEmptyData update(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@PathVariable Long taskId,
		@PathVariable Long commentId,
		@Valid @RequestBody TaskCommentUpdateRequest request
	) {
		taskCommentService.update(currentUser, workspaceId, taskId, commentId, request);

		return ApiEmptyData.empty();
	}

	@DeleteMapping("/{commentId}")
	public ApiEmptyData delete(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@PathVariable Long taskId,
		@PathVariable Long commentId
	) {
		taskCommentService.delete(currentUser, workspaceId, taskId, commentId);

		return ApiEmptyData.empty();
	}
}
