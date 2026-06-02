package dev.runtime_lab.flowit.domain.workspace.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspace;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceRepositoryTest {

	private final EntityManager entityManager = mock(EntityManager.class);
	private final JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
	private final WorkspaceRepository repository = new WorkspaceRepository(entityManager, queryFactory);

	@Test
	@SuppressWarnings("unchecked")
	void existsByInviteCodeReturnsTrueWhenFound() {
		JPAQuery<Integer> query = mock(JPAQuery.class);

		when(queryFactory.selectOne()).thenReturn(query);
		when(query.from(QWorkspace.workspace)).thenReturn(query);
		when(query.where(any(Predicate.class))).thenReturn(query);
		when(query.fetchFirst()).thenReturn(1);

		boolean exists = repository.existsByInviteCode("A1B2-C3D4-E5F6");

		assertTrue(exists);
		verify(queryFactory).selectOne();
		verify(query).from(QWorkspace.workspace);
		verify(query).where(any(Predicate.class));
		verify(query).fetchFirst();
	}

	@Test
	@SuppressWarnings("unchecked")
	void existsByInviteCodeReturnsFalseWhenMissing() {
		JPAQuery<Integer> query = mock(JPAQuery.class);

		when(queryFactory.selectOne()).thenReturn(query);
		when(query.from(QWorkspace.workspace)).thenReturn(query);
		when(query.where(any(Predicate.class))).thenReturn(query);
		when(query.fetchFirst()).thenReturn(null);

		boolean exists = repository.existsByInviteCode("A1B2-C3D4-E5F6");

		assertFalse(exists);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findActiveByIdReturnsActiveWorkspace() {
		JPAQuery<Workspace> query = mock(JPAQuery.class);
		Workspace workspace = Workspace.builder()
			.id(10L)
			.name("Flowit")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(queryFactory.selectFrom(QWorkspace.workspace)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.fetchOne()).thenReturn(workspace);

		Optional<Workspace> found = repository.findActiveById(10L);

		assertTrue(found.isPresent());
		assertTrue(found.get() == workspace);
		verify(queryFactory).selectFrom(QWorkspace.workspace);
		verify(query).where(any(Predicate.class), any(Predicate.class));
		verify(query).fetchOne();
	}

	@Test
	@SuppressWarnings("unchecked")
	void findActiveByIdForUpdateReturnsWorkspaceWithPessimisticWriteLock() {
		JPAQuery<Workspace> query = mock(JPAQuery.class);
		Workspace workspace = Workspace.builder()
			.id(10L)
			.name("Flowit")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(queryFactory.selectFrom(QWorkspace.workspace)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.setLockMode(LockModeType.PESSIMISTIC_WRITE)).thenReturn(query);
		when(query.fetchOne()).thenReturn(workspace);

		Optional<Workspace> found = repository.findActiveByIdForUpdate(10L);

		assertTrue(found.isPresent());
		assertTrue(found.get() == workspace);
		verify(queryFactory).selectFrom(QWorkspace.workspace);
		verify(query).where(any(Predicate.class), any(Predicate.class));
		verify(query).setLockMode(LockModeType.PESSIMISTIC_WRITE);
		verify(query).fetchOne();
	}
}
