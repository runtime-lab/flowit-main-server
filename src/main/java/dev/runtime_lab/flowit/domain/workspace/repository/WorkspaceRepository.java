package dev.runtime_lab.flowit.domain.workspace.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspace;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
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

	public Optional<Workspace> findActiveById(Long id) {
		QWorkspace workspace = QWorkspace.workspace;

		return Optional.ofNullable(
			queryFactory.selectFrom(workspace)
				.where(
					workspace.id.eq(id),
					workspace.deletedAt.isNull()
				)
				.fetchOne()
		);
	}

	public Optional<Workspace> findActiveByIdForUpdate(Long id) {
		QWorkspace workspace = QWorkspace.workspace;

		return Optional.ofNullable(
			queryFactory.selectFrom(workspace)
				.where(
					workspace.id.eq(id),
					workspace.deletedAt.isNull()
				)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
	}
}
