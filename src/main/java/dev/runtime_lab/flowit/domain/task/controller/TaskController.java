package dev.runtime_lab.flowit.domain.task.controller;

import dev.runtime_lab.flowit.domain.task.dto.TaskCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskDetailResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskHistoryResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskProgressUpdateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskSearchRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskStatusUpdateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskSummaryResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskUpdateRequest;
import dev.runtime_lab.flowit.domain.task.service.TaskService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUser;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.response.ApiCreatedData;
import dev.runtime_lab.flowit.global.web.response.ApiEmptyData;
import dev.runtime_lab.flowit.global.web.response.ApiListData;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/workspaces/{workspaceId}/tasks")
@RequiredArgsConstructor
public class TaskController {

	private final TaskService taskService;

	@PostMapping
	public ResponseEntity<ApiCreatedData> create(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@Valid @RequestBody TaskCreateRequest request
	) {
		TaskCreateResponse response = taskService.create(currentUser, workspaceId, request);

		return ResponseEntity
			.created(URI.create("/v1/workspaces/%d/tasks/%d".formatted(workspaceId, response.id())))
			.body(ApiCreatedData.afterCreated(response.id()));
	}

	@GetMapping
	public ApiListData<TaskSummaryResponse> tasks(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@Valid @ModelAttribute TaskSearchRequest request
	) {
		return taskService.tasks(currentUser, workspaceId, request.toQuery());
	}

	@GetMapping("/{taskId}")
	public TaskDetailResponse get(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@PathVariable Long taskId
	) {
		return taskService.get(currentUser, workspaceId, taskId);
	}

	@PatchMapping("/{taskId}")
	public ApiEmptyData update(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@PathVariable Long taskId,
		@Valid @RequestBody TaskUpdateRequest request
	) {
		taskService.update(currentUser, workspaceId, taskId, request);

		return ApiEmptyData.empty();
	}

	@PatchMapping("/{taskId}/progress")
	public ApiEmptyData updateProgress(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@PathVariable Long taskId,
		@Valid @RequestBody TaskProgressUpdateRequest request
	) {
		taskService.updateProgress(currentUser, workspaceId, taskId, request);

		return ApiEmptyData.empty();
	}

	@PatchMapping("/{taskId}/status")
	public ApiEmptyData updateStatus(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@PathVariable Long taskId,
		@Valid @RequestBody TaskStatusUpdateRequest request
	) {
		taskService.updateStatus(currentUser, workspaceId, taskId, request);

		return ApiEmptyData.empty();
	}

	@GetMapping("/{taskId}/histories")
	public ApiListData<TaskHistoryResponse> taskHistories(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@PathVariable Long taskId,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size
	) {
		return taskService.taskHistories(currentUser, workspaceId, taskId, page, size);
	}
}
