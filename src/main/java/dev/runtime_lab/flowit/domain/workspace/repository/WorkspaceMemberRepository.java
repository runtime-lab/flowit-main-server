package dev.runtime_lab.flowit.domain.workspace.repository;

import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.user.dto.UserMeWorkspaceResponse;
import dev.runtime_lab.flowit.domain.user.entity.QUser;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspace;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WorkspaceMemberRepository extends CustomJpaRepo<WorkspaceMember, Long> {

	private final JPAQueryFactory queryFactory;

	public WorkspaceMemberRepository(EntityManager entityManager, JPAQueryFactory queryFactory) {
		super(WorkspaceMember.class, entityManager);
		this.queryFactory = queryFactory;
	}

	public boolean existsActiveOwnerByWorkspaceAndUser(Workspace workspace, User user) {
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;

		return queryFactory.selectOne()
			.from(workspaceMember)
			.where(
				workspaceMember.workspace.eq(workspace),
				workspaceMember.user.eq(user),
				workspaceMember.role.eq(WorkspaceMemberRole.OWNER),
				workspaceMember.deletedAt.isNull()
			)
			.fetchFirst() != null;
	}

	public Optional<WorkspaceMember> findActiveByWorkspaceIdAndUserIdForUpdate(Long workspaceId, Long userId) {
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;

		return Optional.ofNullable(
			queryFactory.selectFrom(workspaceMember)
				.where(
					workspaceMember.workspace.id.eq(workspaceId),
					workspaceMember.user.id.eq(userId),
					workspaceMember.deletedAt.isNull()
				)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
	}

	public Optional<WorkspaceMember> findActiveByWorkspaceIdAndMemberIdForUpdate(Long workspaceId, Long memberId) {
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;

		return Optional.ofNullable(
			queryFactory.selectFrom(workspaceMember)
				.where(
					workspaceMember.workspace.id.eq(workspaceId),
					workspaceMember.id.eq(memberId),
					workspaceMember.deletedAt.isNull()
				)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
	}

	public Optional<WorkspaceMember> findActiveByWorkspaceIdAndUserId(Long workspaceId, Long userId) {
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;

		return Optional.ofNullable(
			queryFactory.selectFrom(workspaceMember)
				.where(
					workspaceMember.workspace.id.eq(workspaceId),
					workspaceMember.user.id.eq(userId),
					workspaceMember.deletedAt.isNull()
				)
				.fetchOne()
		);
	}

	public List<WorkspaceMemberResponse> findActiveMembersByWorkspaceId(Long workspaceId) {
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;
		QUser user = QUser.user;
		NumberExpression<Integer> roleOrder = new CaseBuilder()
			.when(workspaceMember.role.eq(WorkspaceMemberRole.OWNER)).then(0)
			.when(workspaceMember.role.eq(WorkspaceMemberRole.ADMIN)).then(1)
			.otherwise(2);

		return queryFactory.select(Projections.constructor(
				WorkspaceMemberResponse.class,
				workspaceMember.id,
				user.name,
				user.email,
				user.status,
				workspaceMember.role
			))
			.from(workspaceMember)
			.join(workspaceMember.user, user)
			.where(
				workspaceMember.workspace.id.eq(workspaceId),
				workspaceMember.deletedAt.isNull(),
				user.deletedAt.isNull()
			)
			.orderBy(roleOrder.asc(), user.name.asc(), user.id.asc())
			.fetch();
	}

	public long countActiveOwnersByWorkspaceId(Long workspaceId) {
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;

		Long count = queryFactory.select(workspaceMember.id.count())
			.from(workspaceMember)
			.where(
				workspaceMember.workspace.id.eq(workspaceId),
				workspaceMember.role.eq(WorkspaceMemberRole.OWNER),
				workspaceMember.deletedAt.isNull()
			)
			.fetchOne();

		return count == null ? 0L : count;
	}

	public long softDeleteActiveByWorkspaceId(Long workspaceId, Long deletedAt) {
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;

		return queryFactory.update(workspaceMember)
			.set(workspaceMember.updatedAt, deletedAt)
			.set(workspaceMember.deletedAt, deletedAt)
			.where(
				workspaceMember.workspace.id.eq(workspaceId),
				workspaceMember.deletedAt.isNull()
			)
			.execute();
	}

	public List<UserMeWorkspaceResponse> findActiveUserWorkspaces(Long userId) {
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;
		QWorkspaceMember workspaceMemberCount = new QWorkspaceMember("workspaceMemberCount");
		QWorkspace workspace = QWorkspace.workspace;

		return queryFactory.select(Projections.constructor(
				UserMeWorkspaceResponse.class,
				workspace.id,
				workspace.name,
				workspace.description,
				JPAExpressions.select(workspaceMemberCount.id.count())
					.from(workspaceMemberCount)
					.where(
						workspaceMemberCount.workspace.id.eq(workspace.id),
						workspaceMemberCount.deletedAt.isNull()
					),
				workspaceMember.role,
				workspaceMember.joinedAt
			))
			.from(workspaceMember)
			.join(workspaceMember.workspace, workspace)
			.where(
				workspaceMember.user.id.eq(userId),
				workspaceMember.deletedAt.isNull(),
				workspace.deletedAt.isNull()
			)
			.orderBy(workspaceMember.joinedAt.asc(), workspaceMember.id.asc())
			.fetch();
	}
}
