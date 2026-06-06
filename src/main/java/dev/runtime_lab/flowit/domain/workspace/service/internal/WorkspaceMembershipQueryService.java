package dev.runtime_lab.flowit.domain.workspace.service.internal;

import dev.runtime_lab.flowit.domain.workspace.repository.projection.WorkspaceMembershipProjection;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceMembershipSummary;
import dev.runtime_lab.flowit.global.stereotype.InternalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@InternalService
@RequiredArgsConstructor
public class WorkspaceMembershipQueryService {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional(readOnly = true)
	public List<WorkspaceMembershipSummary> findActiveMembershipSummaries(Long userId) {
		return workspaceMemberRepository.findActiveMembershipsByUserId(userId)
			.stream()
			.map(this::summary)
			.toList();
	}

	private WorkspaceMembershipSummary summary(WorkspaceMembershipProjection projection) {
		return new WorkspaceMembershipSummary(
			projection.workspaceId(),
			projection.workspaceName(),
			projection.workspaceDescription(),
			projection.memberCount(),
			projection.role(),
			projection.joinedAt()
		);
	}
}
