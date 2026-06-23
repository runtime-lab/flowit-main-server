package dev.runtime_lab.flowit.domain.task.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.runtime_lab.flowit.domain.task.dto.TaskListQuery;
import dev.runtime_lab.flowit.domain.task.entity.QTask;
import dev.runtime_lab.flowit.domain.task.entity.QTaskTag;
import dev.runtime_lab.flowit.domain.task.entity.Task;
import dev.runtime_lab.flowit.domain.workspace.entity.QWorkspaceMember;
import dev.runtime_lab.flowit.global.jpa.repository.CustomJpaRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository extends CustomJpaRepo<Task, Long> {

	private final JPAQueryFactory queryFactory;

	public TaskRepository(EntityManager entityManager, JPAQueryFactory queryFactory) {
		super(Task.class, entityManager);
		this.queryFactory = queryFactory;
	}

	public Optional<Task> findActiveByWorkspaceIdAndTaskId(Long workspaceId, Long taskId) {
		QTask task = QTask.task;
		QWorkspaceMember assignee = new QWorkspaceMember("assignee");

		return Optional.ofNullable(
			queryFactory.selectFrom(task)
				.leftJoin(task.assignee, assignee).fetchJoin()
				.leftJoin(assignee.user).fetchJoin()
				.join(task.createdBy).fetchJoin()
				.join(task.workspace).fetchJoin()
				.where(
					task.workspace.id.eq(workspaceId),
					task.id.eq(taskId),
					task.deletedAt.isNull()
				)
				.fetchOne()
		);
	}

	public Optional<Task> findActiveByWorkspaceIdAndTaskIdForUpdate(Long workspaceId, Long taskId) {
		QTask task = QTask.task;
		QWorkspaceMember assignee = new QWorkspaceMember("assignee");

		return Optional.ofNullable(
			queryFactory.selectFrom(task)
				.leftJoin(task.assignee, assignee).fetchJoin()
				.leftJoin(assignee.user).fetchJoin()
				.join(task.createdBy).fetchJoin()
				.join(task.workspace).fetchJoin()
				.where(
					task.workspace.id.eq(workspaceId),
					task.id.eq(taskId),
					task.deletedAt.isNull()
				)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
	}

	public List<Task> findActiveByWorkspaceId(Long workspaceId, TaskListQuery query, String normalizedTag) {
		QTask task = QTask.task;
		QWorkspaceMember assignee = new QWorkspaceMember("assignee");

		return queryFactory.selectFrom(task)
			.leftJoin(task.assignee, assignee).fetchJoin()
			.leftJoin(assignee.user).fetchJoin()
			.join(task.workspace).fetchJoin()
			.where(condition(workspaceId, query, normalizedTag))
			.orderBy(task.updatedAt.desc(), task.id.desc())
			.offset((long) query.pageOrDefault() * query.sizeOrDefault())
			.limit(query.sizeOrDefault())
			.fetch();
	}

	public long countActiveByWorkspaceId(Long workspaceId, TaskListQuery query, String normalizedTag) {
		QTask task = QTask.task;

		Long count = queryFactory.select(task.id.count())
			.from(task)
			.where(condition(workspaceId, query, normalizedTag))
			.fetchOne();

		return count == null ? 0L : count;
	}

	public Optional<Task> findActiveByWorkspaceIdAndTitle(Long workspaceId, String title) {
		QTask task = QTask.task;

		return Optional.ofNullable(
			queryFactory.selectFrom(task)
				.where(
					task.workspace.id.eq(workspaceId),
					task.title.eq(title),
					task.deletedAt.isNull()
				)
				.fetchFirst()
		);
	}

	private BooleanBuilder condition(Long workspaceId, TaskListQuery query, String normalizedTag) {
		QTask task = QTask.task;
		QTaskTag taskTag = QTaskTag.taskTag;
		BooleanBuilder builder = new BooleanBuilder()
			.and(task.workspace.id.eq(workspaceId))
			.and(task.deletedAt.isNull());

		if (query.status() != null) {
			builder.and(task.status.eq(query.status()));
		}
		if (query.assigneeMemberId() != null) {
			builder.and(task.assignee.id.eq(query.assigneeMemberId()));
		}
		if (query.keyword() != null && !query.keyword().isBlank()) {
			String keyword = query.keyword().trim();
			builder.and(task.title.containsIgnoreCase(keyword)
				.or(task.descriptionMarkdown.containsIgnoreCase(keyword)));
		}
		if (query.dueFrom() != null) {
			builder.and(task.dueDate.goe(query.dueFrom()));
		}
		if (query.dueTo() != null) {
			builder.and(task.dueDate.loe(query.dueTo()));
		}
		if (normalizedTag != null) {
			builder.and(JPAExpressions.selectOne()
				.from(taskTag)
				.where(
					taskTag.task.eq(task),
					taskTag.normalizedName.eq(normalizedTag)
				)
				.exists());
		}

		return builder;
	}
}
