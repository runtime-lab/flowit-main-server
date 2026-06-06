package dev.runtime_lab.flowit.domain.user.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.user.entity.QUser;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRepositoryTest {

	private final EntityManager entityManager = mock(EntityManager.class);
	private final JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
	private final UserRepository repository = new UserRepository(entityManager, queryFactory);

	@Test
	@SuppressWarnings("unchecked")
	void findActiveByEmailReturnsUserWhenFound() {
		JPAQuery<User> query = mock(JPAQuery.class);
		User user = User.builder()
			.email("user@example.com")
			.passwordHash("hash")
			.name("user")
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(queryFactory.selectFrom(QUser.user)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.fetchOne()).thenReturn(user);

		Optional<User> found = repository.findActiveByEmail("user@example.com");

		assertTrue(found.isPresent());
		assertSame(user, found.get());
		verify(queryFactory).selectFrom(QUser.user);
		verify(query).where(any(Predicate.class), any(Predicate.class));
		verify(query).fetchOne();
	}

	@Test
	@SuppressWarnings("unchecked")
	void existsActiveByEmailReturnsFalseWhenMissing() {
		JPAQuery<User> query = mock(JPAQuery.class);

		when(queryFactory.selectFrom(QUser.user)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.fetchOne()).thenReturn(null);

		boolean exists = repository.existsActiveByEmail("missing@example.com");

		assertFalse(exists);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findActiveByIdReturnsUserWhenFound() {
		JPAQuery<User> query = mock(JPAQuery.class);
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("user")
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(queryFactory.selectFrom(QUser.user)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.fetchOne()).thenReturn(user);

		Optional<User> found = repository.findActiveById(1L);

		assertTrue(found.isPresent());
		assertSame(user, found.get());
		verify(queryFactory).selectFrom(QUser.user);
		verify(query).where(any(Predicate.class), any(Predicate.class));
		verify(query).fetchOne();
	}

	@Test
	@SuppressWarnings("unchecked")
	void findActiveByStatusReturnsUsers() {
		JPAQuery<User> query = mock(JPAQuery.class);
		List<User> users = List.of(
			User.builder()
				.email("user@example.com")
				.passwordHash("hash")
				.name("user")
				.createdAt(1L)
				.updatedAt(1L)
				.build()
		);

		when(queryFactory.selectFrom(QUser.user)).thenReturn(query);
		when(query.where(any(Predicate.class), any(Predicate.class))).thenReturn(query);
		when(query.fetch()).thenReturn(users);

		List<User> found = repository.findActiveByStatus(UserStatus.ACTIVE);

		assertEquals(users, found);
		verify(queryFactory).selectFrom(QUser.user);
		verify(query).where(any(Predicate.class), any(Predicate.class));
		verify(query).fetch();
	}
}
