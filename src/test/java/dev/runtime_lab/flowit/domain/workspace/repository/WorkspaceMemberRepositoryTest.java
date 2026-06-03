package dev.runtime_lab.flowit.domain.workspace.repository;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import dev.runtime_lab.flowit.domain.user.dto.UserMeWorkspaceResponse;
import dev.runtime_lab.flowit.domain.user.entity.QUser;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspace;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceMemberRepositoryTest {

	private final EntityManager entityManager = mock(EntityManager.class);
	private final JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
	private final WorkspaceMemberRepository repository = new WorkspaceMemberRepository(entityManager, queryFactory);

	@Test
	@SuppressWarnings("unchecked")
	void existsActiveOwnerByWorkspaceAndUserReturnsTrueWhenFound() {
		JPAQuery<Integer> query = mock(JPAQuery.class);
		User user = activeUser();
		Workspace workspace = workspace(user);

		when(queryFactory.selectOne()).thenReturn(query);
		when(query.from(QWorkspaceMember.workspaceMember)).thenReturn(query);
		when(query.where(
			any(Predicate.class),
			any(Predicate.class),
			any(Predicate.class),
			any(Predicate.class)
		)).thenReturn(query);
		when(query.fetchFirst()).thenReturn(1);

		boolean exists = repository.existsActiveOwnerByWorkspaceAndUser(workspace, user);

		assertTrue(exists);
		verify(queryFactory).selectOne();
		verify(query).from(QWorkspaceMember.workspaceMember);
		verify(query).where(
			any(Predicate.class),
			any(Predicate.class),
			any(Predicate.class),
			any(Predicate.class)
		);
		verify(query).fetchFirst();
	}

	@Test
	@SuppressWarnings("unchecked")
	void findActiveByWorkspaceIdAndUserIdForUpdateReturnsLockedActiveMembership() {
		JPAQuery<WorkspaceMember> query = mock(JPAQuery.class);
		User user = activeUser();
		Workspace workspace = workspace(user);
		WorkspaceMember workspaceMember = WorkspaceMember.builder()
			.id(100L)
			.workspace(workspace)
			.user(user)
			.role(WorkspaceMemberRole.ADMIN)
			.joinedAt(1L)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(queryFactory.selectFrom(QWorkspaceMember.workspaceMember)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.setLockMode(LockModeType.PESSIMISTIC_WRITE)).thenReturn(query);
		when(query.fetchOne()).thenReturn(workspaceMember);

		var found = repository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L);

		assertTrue(found.isPresent());
		assertEquals(workspaceMember, found.get());
		verify(queryFactory).selectFrom(QWorkspaceMember.workspaceMember);
		verify(query).where(any(Predicate.class), any(Predicate.class), any(Predicate.class));
		verify(query).setLockMode(LockModeType.PESSIMISTIC_WRITE);
		verify(query).fetchOne();
	}

	@Test
	@SuppressWarnings("unchecked")
	void findActiveByWorkspaceIdAndMemberIdForUpdateReturnsLockedActiveMembership() {
		JPAQuery<WorkspaceMember> query = mock(JPAQuery.class);
		User user = activeUser();
		Workspace workspace = workspace(user);
		WorkspaceMember workspaceMember = WorkspaceMember.builder()
			.id(100L)
			.workspace(workspace)
			.user(user)
			.role(WorkspaceMemberRole.ADMIN)
			.joinedAt(1L)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(queryFactory.selectFrom(QWorkspaceMember.workspaceMember)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.setLockMode(LockModeType.PESSIMISTIC_WRITE)).thenReturn(query);
		when(query.fetchOne()).thenReturn(workspaceMember);

		var found = repository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 100L);

		assertTrue(found.isPresent());
		assertEquals(workspaceMember, found.get());
		verify(queryFactory).selectFrom(QWorkspaceMember.workspaceMember);
		verify(query).where(any(Predicate.class), any(Predicate.class), any(Predicate.class));
		verify(query).setLockMode(LockModeType.PESSIMISTIC_WRITE);
		verify(query).fetchOne();
	}

	@Test
	@SuppressWarnings("unchecked")
	void findActiveByWorkspaceIdAndUserIdReturnsActiveMembership() {
		JPAQuery<WorkspaceMember> query = mock(JPAQuery.class);
		User user = activeUser();
		Workspace workspace = workspace(user);
		WorkspaceMember workspaceMember = WorkspaceMember.builder()
			.id(100L)
			.workspace(workspace)
			.user(user)
			.role(WorkspaceMemberRole.ADMIN)
			.joinedAt(1L)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(queryFactory.selectFrom(QWorkspaceMember.workspaceMember)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.fetchOne()).thenReturn(workspaceMember);

		var found = repository.findActiveByWorkspaceIdAndUserId(10L, 1L);

		assertTrue(found.isPresent());
		assertEquals(workspaceMember, found.get());
		verify(queryFactory).selectFrom(QWorkspaceMember.workspaceMember);
		verify(query).where(any(Predicate.class), any(Predicate.class), any(Predicate.class));
		verify(query).fetchOne();
	}

	@Test
	@SuppressWarnings("unchecked")
	void findActiveMembersByWorkspaceIdReturnsSortedProjectedMembers() {
		JPAQuery<WorkspaceMemberResponse> query = mock(JPAQuery.class);
		List<WorkspaceMemberResponse> responses = List.of(
			new WorkspaceMemberResponse(100L, "Owner", "owner@example.com", UserStatus.ACTIVE, WorkspaceMemberRole.OWNER),
			new WorkspaceMemberResponse(101L, "Admin", "admin@example.com", UserStatus.ACTIVE, WorkspaceMemberRole.ADMIN),
			new WorkspaceMemberResponse(102L, "Member", "member@example.com", UserStatus.ACTIVE, WorkspaceMemberRole.MEMBER)
		);

		when(queryFactory.select(org.mockito.ArgumentMatchers.<ConstructorExpression<WorkspaceMemberResponse>>any()))
			.thenReturn(query);
		when(query.from(QWorkspaceMember.workspaceMember)).thenReturn(query);
		when(query.join(QWorkspaceMember.workspaceMember.user, QUser.user)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.orderBy(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
			.thenReturn(query);
		when(query.fetch()).thenReturn(responses);

		List<WorkspaceMemberResponse> found = repository.findActiveMembersByWorkspaceId(10L);

		assertEquals(responses, found);
		verify(queryFactory).select(org.mockito.ArgumentMatchers.<ConstructorExpression<WorkspaceMemberResponse>>any());
		verify(query).from(QWorkspaceMember.workspaceMember);
		verify(query).join(QWorkspaceMember.workspaceMember.user, QUser.user);
		verify(query).where(any(Predicate.class), any(Predicate.class), any(Predicate.class));
		verify(query).orderBy(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
		verify(query).fetch();
	}

	@Test
	@SuppressWarnings("unchecked")
	void countActiveOwnersByWorkspaceIdReturnsOwnerCount() {
		JPAQuery<Long> query = mock(JPAQuery.class);

		when(queryFactory.select(org.mockito.ArgumentMatchers.<com.querydsl.core.types.Expression<Long>>any()))
			.thenReturn(query);
		when(query.from(QWorkspaceMember.workspaceMember)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.fetchOne()).thenReturn(2L);

		long count = repository.countActiveOwnersByWorkspaceId(10L);

		assertEquals(2L, count);
		verify(queryFactory).select(org.mockito.ArgumentMatchers.<com.querydsl.core.types.Expression<Long>>any());
		verify(query).from(QWorkspaceMember.workspaceMember);
		verify(query).where(any(Predicate.class), any(Predicate.class), any(Predicate.class));
		verify(query).fetchOne();
	}

	@Test
	@SuppressWarnings("unchecked")
	void softDeleteActiveByWorkspaceIdUpdatesActiveRows() {
		JPAUpdateClause update = mock(JPAUpdateClause.class);

		when(queryFactory.update(QWorkspaceMember.workspaceMember)).thenReturn(update);
		when(update.set(QWorkspaceMember.workspaceMember.updatedAt, 1779889000L)).thenReturn(update);
		when(update.set(QWorkspaceMember.workspaceMember.deletedAt, 1779889000L)).thenReturn(update);
		when(update.where(any(Predicate.class), any(Predicate.class))).thenReturn(update);
		when(update.execute()).thenReturn(3L);

		long updatedCount = repository.softDeleteActiveByWorkspaceId(10L, 1779889000L);

		assertEquals(3L, updatedCount);
		verify(queryFactory).update(QWorkspaceMember.workspaceMember);
		verify(update).set(QWorkspaceMember.workspaceMember.updatedAt, 1779889000L);
		verify(update).set(QWorkspaceMember.workspaceMember.deletedAt, 1779889000L);
		verify(update).where(any(Predicate.class), any(Predicate.class));
		verify(update).execute();
	}

	@Test
	@SuppressWarnings("unchecked")
	void findActiveUserWorkspacesReturnsProjectedWorkspaceMemberships() {
		JPAQuery<UserMeWorkspaceResponse> query = mock(JPAQuery.class);
		List<UserMeWorkspaceResponse> responses = List.of(
			new UserMeWorkspaceResponse(10L, "Flowit", "Team workspace", 3L, WorkspaceMemberRole.OWNER, 2L)
		);

		when(queryFactory.select(org.mockito.ArgumentMatchers.<ConstructorExpression<UserMeWorkspaceResponse>>any()))
			.thenReturn(query);
		when(query.from(QWorkspaceMember.workspaceMember)).thenReturn(query);
		when(query.join(QWorkspaceMember.workspaceMember.workspace, QWorkspace.workspace)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.orderBy(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
		when(query.fetch()).thenReturn(responses);

		List<UserMeWorkspaceResponse> found = repository.findActiveUserWorkspaces(1L);

		assertEquals(responses, found);
		verify(queryFactory).select(org.mockito.ArgumentMatchers.<ConstructorExpression<UserMeWorkspaceResponse>>any());
		verify(query).from(QWorkspaceMember.workspaceMember);
		verify(query).join(QWorkspaceMember.workspaceMember.workspace, QWorkspace.workspace);
		verify(query).where(any(Predicate.class), any(Predicate.class), any(Predicate.class));
		verify(query).orderBy(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
		verify(query).fetch();
	}

	private User activeUser() {
		return User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private Workspace workspace(User creator) {
		return Workspace.builder()
			.id(10L)
			.name("Flowit")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdBy(creator)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
