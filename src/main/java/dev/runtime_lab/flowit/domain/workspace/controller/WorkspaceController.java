package dev.runtime_lab.flowit.domain.workspace.controller;

import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordSearchRequest;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordResponse;
import dev.runtime_lab.flowit.domain.activity.service.WorkspaceActivityService;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceUpdateRequest;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUser;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.response.ApiCreatedData;
import dev.runtime_lab.flowit.global.web.response.ApiEmptyData;
import dev.runtime_lab.flowit.global.web.response.ApiListData;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

	private final WorkspaceService workspaceService;
	private final WorkspaceActivityService workspaceActivityService;

	@PostMapping
	public ResponseEntity<ApiCreatedData> create(
		@AuthenticatedUser CurrentUser currentUser,
		@Valid @RequestBody WorkspaceCreateRequest request
	) {
		WorkspaceCreateResponse response = workspaceService.create(currentUser, request);

		return ResponseEntity
			.created(URI.create("/v1/workspaces/%d".formatted(response.id())))
			.body(ApiCreatedData.afterCreated(response.id()));
	}

	@PatchMapping("/{workspaceId}")
	public WorkspaceResponse update(
			@AuthenticatedUser CurrentUser currentUser,
			@PathVariable Long workspaceId,
			@RequestBody WorkspaceUpdateRequest request
	) {
		return workspaceService.update(currentUser, workspaceId, request);
	}

	@GetMapping("/{workspaceId}")
	public WorkspaceResponse get(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId
	) {
		return workspaceService.get(currentUser, workspaceId);
	}

	@GetMapping("/{workspaceId}/activity-records")
	public ApiListData<ActivityRecordResponse> activityRecords(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId,
		@Valid @ModelAttribute ActivityRecordSearchRequest request
	) {
		return workspaceActivityService.activityRecords(currentUser, workspaceId, request.toQuery());
	}

	@DeleteMapping("/{workspaceId}")
	public ResponseEntity<ApiEmptyData> delete(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId
	) {
		workspaceService.delete(currentUser, workspaceId);

		return ResponseEntity.ok(ApiEmptyData.empty());
	}
}
