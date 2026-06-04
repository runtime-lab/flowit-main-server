package dev.runtime_lab.flowit.domain.workspace.repository;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestHistory;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class WorkspaceJoinRequestHistoryRepository extends CustomJpaRepo<WorkspaceJoinRequestHistory, Long> {

	public WorkspaceJoinRequestHistoryRepository(EntityManager entityManager) {
		super(WorkspaceJoinRequestHistory.class, entityManager);
	}
}
