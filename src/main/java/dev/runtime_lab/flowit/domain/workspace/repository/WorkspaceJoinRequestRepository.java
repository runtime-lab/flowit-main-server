package dev.runtime_lab.flowit.domain.workspace.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.user.entity.QUser;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspace;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspaceJoinRequestHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WorkspaceJoinRequestRepository extends CustomJpaRepo<WorkspaceJoinRequest, Long> {

	private final JPAQueryFactory queryFactory;

	public WorkspaceJoinRequestRepository(EntityManager entityManager, JPAQueryFactory queryFactory) {
		super(WorkspaceJoinRequest.class, entityManager);
		this.queryFactory = queryFactory;
	}

	public List<WorkspaceJoinRequest> findByWorkspaceIdWithHistories(Long workspaceId) {
		QWorkspaceJoinRequest joinRequest = QWorkspaceJoinRequest.workspaceJoinRequest;
		QWorkspaceJoinRequestHistory history = QWorkspaceJoinRequestHistory.workspaceJoinRequestHistory;
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;
		QWorkspace workspace = QWorkspace.workspace;
		QUser user = QUser.user;

		return queryFactory.selectFrom(joinRequest)
			.distinct()
			.join(joinRequest.workspace, workspace)
			.fetchJoin()
			.join(joinRequest.user, user)
			.fetchJoin()
			.leftJoin(joinRequest.workspaceMember, workspaceMember)
			.fetchJoin()
			.leftJoin(joinRequest.histories, history)
			.fetchJoin()
			.where(
				joinRequest.workspace.id.eq(workspaceId),
				workspace.deletedAt.isNull()
			)
			.orderBy(joinRequest.requestedAt.desc(), joinRequest.id.desc(), history.changedAt.asc(), history.id.asc())
			.fetch();
	}

	public Optional<WorkspaceJoinRequest> findByIdForUpdate(Long id) {
		QWorkspaceJoinRequest joinRequest = QWorkspaceJoinRequest.workspaceJoinRequest;
		QWorkspaceJoinRequestHistory history = QWorkspaceJoinRequestHistory.workspaceJoinRequestHistory;
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;
		QWorkspace workspace = QWorkspace.workspace;
		QUser user = QUser.user;

		return Optional.ofNullable(
			queryFactory.selectFrom(joinRequest)
				.distinct()
				.join(joinRequest.workspace, workspace)
				.fetchJoin()
				.join(joinRequest.user, user)
				.fetchJoin()
				.leftJoin(joinRequest.workspaceMember, workspaceMember)
				.fetchJoin()
				.leftJoin(joinRequest.histories, history)
				.fetchJoin()
				.where(joinRequest.id.eq(id))
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
	}
}
