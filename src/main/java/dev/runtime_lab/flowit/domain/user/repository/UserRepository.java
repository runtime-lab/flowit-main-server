package dev.runtime_lab.flowit.domain.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.user.entity.QUser;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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

	public Optional<User> findActiveByIdForUpdate(Long id) {
		QUser user = QUser.user;

		return Optional.ofNullable(
			queryFactory.selectFrom(user)
				.where(
					user.id.eq(id),
					user.deletedAt.isNull()
				)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
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
}
