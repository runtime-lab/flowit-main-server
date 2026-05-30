package dev.runtime_lab.flowit.domain.workspace.repository;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class WorkspaceMemberRepository extends CustomJpaRepo<WorkspaceMember, Long> {

	public WorkspaceMemberRepository(EntityManager entityManager) {
		super(WorkspaceMember.class, entityManager);
	}
}
