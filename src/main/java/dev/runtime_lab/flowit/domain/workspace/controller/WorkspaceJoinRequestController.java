package dev.runtime_lab.flowit.domain.workspace.controller;

import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinByInviteCodeRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestResultResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceJoinRequestsResponse;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceJoinRequestService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUser;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceJoinRequestController {

	private final WorkspaceJoinRequestService workspaceJoinRequestService;

	@PostMapping("/join-requests/invite-code")
	public ResponseEntity<WorkspaceJoinRequestResultResponse> joinByInviteCode(
		@AuthenticatedUser CurrentUser currentUser,
		@Valid @RequestBody WorkspaceJoinByInviteCodeRequest request
	) {
		WorkspaceJoinRequestResultResponse response = workspaceJoinRequestService.joinByInviteCode(currentUser, request);

		return ResponseEntity
			.created(URI.create("/v1/workspaces/%d/join-requests/%d".formatted(
				response.workspaceId(),
				response.joinRequestId()
			)))
			.body(response);
	}

	@GetMapping("/{workspaceId}/join-requests")
	public WorkspaceJoinRequestsResponse requests(
		@AuthenticatedUser CurrentUser currentUser,
		@PathVariable Long workspaceId
	) {
		return workspaceJoinRequestService.requests(currentUser, workspaceId);
	}
}
