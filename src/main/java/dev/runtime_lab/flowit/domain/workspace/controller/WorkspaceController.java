package dev.runtime_lab.flowit.domain.workspace.controller;

import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceCreateService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUser;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.response.ApiCreatedData;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

	private final WorkspaceCreateService workspaceCreateService;

	@PostMapping
	public ResponseEntity<ApiCreatedData> create(
		@AuthenticatedUser CurrentUser currentUser,
		@Valid @RequestBody WorkspaceCreateRequest request
	) {
		WorkspaceCreateResponse response = workspaceCreateService.create(currentUser, request);

		return ResponseEntity
			.created(URI.create("/v1/workspaces/%d".formatted(response.id())))
			.body(ApiCreatedData.afterCreated(response.id()));
	}
}
