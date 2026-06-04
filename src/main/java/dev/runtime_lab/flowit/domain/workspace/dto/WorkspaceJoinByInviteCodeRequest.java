package dev.runtime_lab.flowit.domain.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceJoinByInviteCodeRequest(
	@NotBlank
	@Size(min = 14, max = 14)
	String inviteCode
) {
}
