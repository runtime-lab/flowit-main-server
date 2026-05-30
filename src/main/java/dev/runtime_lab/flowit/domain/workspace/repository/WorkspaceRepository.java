package dev.runtime_lab.flowit.domain.workspace.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspace;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class WorkspaceRepository extends CustomJpaRepo<Workspace, Long> {

	private final JPAQueryFactory queryFactory;

	public WorkspaceRepository(EntityManager entityManager, JPAQueryFactory queryFactory) {
		super(Workspace.class, entityManager);
		this.queryFactory = queryFactory;
	}

	public boolean existsByInviteCode(String inviteCode) {
		QWorkspace workspace = QWorkspace.workspace;

		return queryFactory.selectOne()
			.from(workspace)
			.where(workspace.inviteCode.eq(inviteCode))
			.fetchFirst() != null;
	}
}
