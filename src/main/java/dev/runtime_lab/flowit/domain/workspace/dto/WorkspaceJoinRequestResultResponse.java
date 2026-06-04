package dev.runtime_lab.flowit.domain.workspace.dto;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestMethod;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;

public record WorkspaceJoinRequestResultResponse(
	Long joinRequestId,
	Long workspaceId,
	String workspaceName,
	Long userId,
	String userName,
	String userEmail,
	Long memberId,
	WorkspaceJoinRequestMethod method,
	String inviteCode,
	WorkspaceJoinRequestStatus status,
	Long joinedAt
) {

	public static WorkspaceJoinRequestResultResponse from(WorkspaceJoinRequest joinRequest) {
		Long memberId = joinRequest.getWorkspaceMember() == null ? null : joinRequest.getWorkspaceMember().getId();

		return new WorkspaceJoinRequestResultResponse(
			joinRequest.getId(),
			joinRequest.getWorkspace().getId(),
			joinRequest.getWorkspace().getName(),
			joinRequest.getUser().getId(),
			joinRequest.getUser().getName(),
			joinRequest.getUser().getEmail(),
			memberId,
			joinRequest.getMethod(),
			joinRequest.getInviteCodeSnapshot(),
			joinRequest.getStatus(),
			joinRequest.getJoinedAt()
		);
	}
}
