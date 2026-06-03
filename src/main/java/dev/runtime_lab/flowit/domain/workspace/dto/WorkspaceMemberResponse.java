package dev.runtime_lab.flowit.domain.workspace.dto;

import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;

public record WorkspaceMemberResponse(
	Long memberId,
	String name,
	String email,
	UserStatus status,
	WorkspaceMemberRole role
) {
}
