package dev.runtime_lab.flowit.domain.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceCreateRequest(
	@NotBlank
	@Size(max = 100)
	String name,

	@Size(max = 500)
	String description
) {
}
