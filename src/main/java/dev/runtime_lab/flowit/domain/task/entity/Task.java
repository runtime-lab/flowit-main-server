package dev.runtime_lab.flowit.domain.task.entity;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(
	name = "tasks",
	indexes = {
		@Index(name = "idx_tasks_workspace_status", columnList = "workspace_id,status"),
		@Index(name = "idx_tasks_workspace_assignee", columnList = "workspace_id,assignee_workspace_member_id"),
		@Index(name = "idx_tasks_workspace_due_date", columnList = "workspace_id,due_date"),
		@Index(name = "idx_tasks_workspace_updated_at", columnList = "workspace_id,updated_at"),
		@Index(name = "idx_tasks_created_by_user_id", columnList = "created_by_user_id"),
		@Index(name = "idx_tasks_deleted_at", columnList = "deleted_at")
	}
)
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "workspace_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_tasks_workspace")
	)
	private Workspace workspace;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "description_markdown", columnDefinition = "TEXT")
	private String descriptionMarkdown;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private TaskStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "priority", nullable = false, length = 30)
	private TaskPriority priority;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "assignee_workspace_member_id",
		foreignKey = @ForeignKey(name = "fk_tasks_assignee_workspace_member")
	)
	private WorkspaceMember assignee;

	@Column(name = "start_date")
	private Long startDate;

	@Column(name = "due_date")
	private Long dueDate;

	@Column(name = "progress", nullable = false)
	@Builder.Default
	private Integer progress = 0;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "created_by_user_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_tasks_created_by_user")
	)
	private User createdBy;

	@Column(name = "created_at", nullable = false)
	private Long createdAt;

	@Column(name = "updated_at", nullable = false)
	private Long updatedAt;

	@Column(name = "deleted_at")
	private Long deletedAt;

	public void update(
		String title,
		String descriptionMarkdown,
		TaskStatus status,
		WorkspaceMember assignee,
		TaskPriority priority,
		Long startDate,
		Long dueDate,
		Long updatedAt
	) {
		this.title = title;
		this.descriptionMarkdown = descriptionMarkdown;
		this.status = status;
		this.assignee = assignee;
		this.priority = priority;
		this.startDate = startDate;
		this.dueDate = dueDate;
		this.updatedAt = updatedAt;
	}

	public void updateProgress(Integer progress, Long updatedAt) {
		this.progress = progress;
		this.updatedAt = updatedAt;
	}

	public void updateStatus(TaskStatus status, Long updatedAt) {
		this.status = status;
		this.updatedAt = updatedAt;
	}
}
