package dev.runtime_lab.flowit.domain.workspace.dto;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestMethod;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import java.util.List;

public record WorkspaceJoinRequestResponse(
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
	Long requestedAt,
	Long readyAt,
	Long approvedAt,
	Long joinedAt,
	Long failedAt,
	String failureCode,
	String failureMessage,
	List<WorkspaceJoinRequestHistoryResponse> histories
) {

	public static WorkspaceJoinRequestResponse from(WorkspaceJoinRequest joinRequest) {
		Long memberId = joinRequest.getWorkspaceMember() == null ? null : joinRequest.getWorkspaceMember().getId();

		return new WorkspaceJoinRequestResponse(
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
			joinRequest.getRequestedAt(),
			joinRequest.getReadyAt(),
			joinRequest.getApprovedAt(),
			joinRequest.getJoinedAt(),
			joinRequest.getFailedAt(),
			joinRequest.getFailureCode(),
			joinRequest.getFailureMessage(),
			joinRequest.getHistories().stream()
				.map(WorkspaceJoinRequestHistoryResponse::from)
				.toList()
		);
	}
}
