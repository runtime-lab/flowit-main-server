package dev.runtime_lab.flowit.domain.user.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.user.dto.UserMeResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserMeWorkspaceResponse;
import dev.runtime_lab.flowit.domain.user.entity.QUser;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspace;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends CustomJpaRepo<User, Long> {

	private final JPAQueryFactory queryFactory;

	public UserRepository(EntityManager entityManager, JPAQueryFactory queryFactory) {
		super(User.class, entityManager);
		this.queryFactory = queryFactory;
	}

	public Optional<User> findActiveByEmail(String email) {
		QUser user = QUser.user;

		return Optional.ofNullable(
			queryFactory.selectFrom(user)
				.where(
					user.email.eq(email),
					user.deletedAt.isNull()
				)
				.fetchOne()
		);
	}

	public Optional<User> findActiveById(Long id) {
		QUser user = QUser.user;

		return Optional.ofNullable(
			queryFactory.selectFrom(user)
				.where(
					user.id.eq(id),
					user.deletedAt.isNull()
				)
				.fetchOne()
		);
	}

	public Optional<UserMeResponse> findActiveMeById(Long id) {
		QUser user = QUser.user;
		QWorkspaceMember workspaceMember = QWorkspaceMember.workspaceMember;
		QWorkspaceMember workspaceMemberCount = new QWorkspaceMember("workspaceMemberCount");
		QWorkspace workspace = QWorkspace.workspace;

		List<UserMeProjectionRow> rows = queryFactory
			.select(Projections.constructor(
				UserMeProjectionRow.class,
				user.id,
				user.email,
				user.name,
				user.status,
				user.profileImageFile.id,
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
			.from(user)
			.leftJoin(workspaceMember)
			.on(
				workspaceMember.user.id.eq(user.id),
				workspaceMember.deletedAt.isNull()
			)
			.leftJoin(workspaceMember.workspace, workspace)
			.on(workspace.deletedAt.isNull())
			.where(
				user.id.eq(id),
				user.status.eq(UserStatus.ACTIVE),
				user.deletedAt.isNull()
			)
			.orderBy(workspaceMember.joinedAt.asc().nullsLast(), workspaceMember.id.asc().nullsLast())
			.fetch();

		if (rows.isEmpty()) {
			return Optional.empty();
		}

		UserMeProjectionRow firstRow = rows.get(0);
		List<UserMeWorkspaceResponse> workspaces = rows.stream()
			.filter(UserMeProjectionRow::hasWorkspace)
			.map(UserMeProjectionRow::toWorkspaceResponse)
			.toList();

		return Optional.of(firstRow.toResponse(workspaces));
	}

	public boolean existsActiveByEmail(String email) {
		return findActiveByEmail(email).isPresent();
	}

	public List<User> findActiveByStatus(UserStatus status) {
		QUser user = QUser.user;

		return queryFactory.selectFrom(user)
			.where(
				user.status.eq(status),
				user.deletedAt.isNull()
			)
			.fetch();
	}

	public record UserMeProjectionRow(
		Long id,
		String email,
		String nickname,
		UserStatus status,
		Long profileImageFileId,
		Long workspaceId,
		String workspaceName,
		String workspaceDescription,
		Long workspaceMemberCount,
		WorkspaceMemberRole workspaceRole,
		Long workspaceJoinedAt
	) {

		boolean hasWorkspace() {
			return workspaceId != null;
		}

		UserMeWorkspaceResponse toWorkspaceResponse() {
			return new UserMeWorkspaceResponse(
				workspaceId,
				workspaceName,
				workspaceDescription,
				workspaceMemberCount,
				workspaceRole,
				workspaceJoinedAt
			);
		}

		UserMeResponse toResponse(List<UserMeWorkspaceResponse> workspaces) {
			return new UserMeResponse(
				id,
				email,
				nickname,
				status,
				profileImageFileId,
				workspaces
			);
		}
	}
}
