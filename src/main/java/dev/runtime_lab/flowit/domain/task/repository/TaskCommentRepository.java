package dev.runtime_lab.flowit.domain.task.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.task.entity.QTaskComment;
import dev.runtime_lab.flowit.domain.task.entity.TaskComment;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TaskCommentRepository extends CustomJpaRepo<TaskComment, Long> {

	private final JPAQueryFactory queryFactory;

	public TaskCommentRepository(EntityManager entityManager, JPAQueryFactory queryFactory) {
		super(TaskComment.class, entityManager);
		this.queryFactory = queryFactory;
	}

	public List<TaskComment> findActiveByWorkspaceIdAndTaskId(
		Long workspaceId,
		Long taskId,
		int page,
		int size
	) {
		QTaskComment taskComment = QTaskComment.taskComment;

		return queryFactory.selectFrom(taskComment)
			.join(taskComment.authorWorkspaceMember).fetchJoin()
			.join(taskComment.authorUser).fetchJoin()
			.where(
				taskComment.workspace.id.eq(workspaceId),
				taskComment.task.id.eq(taskId),
				taskComment.deletedAt.isNull()
			)
			.orderBy(taskComment.createdAt.asc(), taskComment.id.asc())
			.offset((long) page * size)
			.limit(size)
			.fetch();
	}

	public long countActiveByWorkspaceIdAndTaskId(Long workspaceId, Long taskId) {
		QTaskComment taskComment = QTaskComment.taskComment;

		Long count = queryFactory.select(taskComment.id.count())
			.from(taskComment)
			.where(
				taskComment.workspace.id.eq(workspaceId),
				taskComment.task.id.eq(taskId),
				taskComment.deletedAt.isNull()
			)
			.fetchOne();

		return count == null ? 0L : count;
	}

	public Optional<TaskComment> findActiveByWorkspaceIdAndTaskIdAndCommentIdForUpdate(
		Long workspaceId,
		Long taskId,
		Long commentId
	) {
		QTaskComment taskComment = QTaskComment.taskComment;

		return Optional.ofNullable(
			queryFactory.selectFrom(taskComment)
				.join(taskComment.authorWorkspaceMember).fetchJoin()
				.join(taskComment.authorUser).fetchJoin()
				.where(
					taskComment.workspace.id.eq(workspaceId),
					taskComment.task.id.eq(taskId),
					taskComment.id.eq(commentId),
					taskComment.deletedAt.isNull()
				)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
	}
}
