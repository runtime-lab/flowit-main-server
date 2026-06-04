package dev.runtime_lab.flowit.domain.workspace.dto;

import java.util.List;

public record WorkspaceJoinRequestsResponse(
	List<WorkspaceJoinRequestResponse> joinRequests
) {
}
