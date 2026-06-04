package dev.runtime_lab.flowit.domain.workspace.dto;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;

public record WorkspaceJoinRequestHistoryResponse(
	Long historyId,
	WorkspaceJoinRequestStatus fromStatus,
	WorkspaceJoinRequestStatus toStatus,
	Long changedByUserId,
	Long changedAt
) {

	public static WorkspaceJoinRequestHistoryResponse from(WorkspaceJoinRequestHistory history) {
		Long changedByUserId = history.getChangedBy() == null ? null : history.getChangedBy().getId();

		return new WorkspaceJoinRequestHistoryResponse(
			history.getId(),
			history.getFromStatus(),
			history.getToStatus(),
			changedByUserId,
			history.getChangedAt()
		);
	}
}
