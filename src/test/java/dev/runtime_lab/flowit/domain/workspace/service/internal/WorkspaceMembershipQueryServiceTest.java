package dev.runtime_lab.flowit.domain.workspace.service.internal;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.repository.projection.WorkspaceMembershipProjection;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import java.util.List;

import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceMembershipSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceMembershipQueryServiceTest {

	private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
	private final WorkspaceMembershipQueryService workspaceMembershipQueryService = new WorkspaceMembershipQueryService(
		workspaceMemberRepository
	);

	@Test
	void findActiveMembershipSummariesConvertsRepositoryProjections() {
		List<WorkspaceMembershipProjection> projections = List.of(
			new WorkspaceMembershipProjection(10L, "Flowit", "Team workspace", 3L, WorkspaceMemberRole.OWNER, 2L)
		);

		when(workspaceMemberRepository.findActiveMembershipsByUserId(1L)).thenReturn(projections);

		List<WorkspaceMembershipSummary> response = workspaceMembershipQueryService.findActiveMembershipSummaries(1L);

		assertEquals(1, response.size());
		assertEquals(10L, response.get(0).workspaceId());
		assertEquals("Flowit", response.get(0).workspaceName());
		assertEquals(WorkspaceMemberRole.OWNER, response.get(0).role());
		verify(workspaceMemberRepository).findActiveMembershipsByUserId(1L);
	}
}
