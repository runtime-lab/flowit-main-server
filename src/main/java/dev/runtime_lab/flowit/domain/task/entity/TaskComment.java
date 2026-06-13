package dev.runtime_lab.flowit.domain.task.entity;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "task_comments",
	indexes = {
		@Index(name = "idx_task_comments_task_created", columnList = "task_id,created_at,id"),
		@Index(name = "idx_task_comments_workspace_created", columnList = "workspace_id,created_at,id"),
		@Index(name = "idx_task_comments_author_member", columnList = "author_workspace_member_id"),
		@Index(name = "idx_task_comments_author_user", columnList = "author_user_id"),
		@Index(name = "idx_task_comments_deleted_at", columnList = "deleted_at")
	}
)
public class TaskComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "workspace_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_task_comments_workspace")
	)
	private Workspace workspace;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "task_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_task_comments_task")
	)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "author_workspace_member_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_task_comments_author_member")
	)
	private WorkspaceMember authorWorkspaceMember;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "author_user_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_task_comments_author_user")
	)
	private User authorUser;

	@Column(name = "author_display_name_snapshot", nullable = false, length = 100)
	private String authorDisplayNameSnapshot;

	@Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
	private String contentMarkdown;

	@Column(name = "edited", nullable = false)
	@Builder.Default
	private boolean edited = false;

	@Column(name = "created_at", nullable = false)
	private Long createdAt;

	@Column(name = "updated_at", nullable = false)
	private Long updatedAt;

	@Column(name = "deleted_at")
	private Long deletedAt;

	public void updateContent(String contentMarkdown, Long updatedAt) {
		this.contentMarkdown = contentMarkdown;
		this.updatedAt = updatedAt;
		this.edited = true;
	}

	public void softDelete(Long deletedAt) {
		this.deletedAt = deletedAt;
		this.updatedAt = deletedAt;
	}
}
