package dev.runtime_lab.flowit.domain.workspace.entity;

import dev.runtime_lab.flowit.domain.user.entity.User;
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
	name = "workspaces",
	indexes = {
		@Index(name = "idx_workspaces_created_by_user_id", columnList = "created_by_user_id"),
		@Index(name = "idx_workspaces_deleted_at", columnList = "deleted_at")
	}
)
public class Workspace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "invite_code", nullable = false, length = 14, unique = true)
	private String inviteCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "created_by_user_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_workspaces_created_by_user")
	)
	private User createdBy;

	@Column(name = "created_at", nullable = false)
	private Long createdAt;

	@Column(name = "updated_at", nullable = false)
	private Long updatedAt;

	@Column(name = "deleted_at")
	private Long deletedAt;

	public void softDelete(Long deletedAt) {
		this.deletedAt = deletedAt;
		this.updatedAt = deletedAt;
	}

	public void setWorkspaceName(String name) {
		this.name = name;
	}

	public void setWorkspaceDescription(String description) {
		this.description = description;
	}

	public void setUpdatedAt(long epoch) {
		this.updatedAt = epoch;
	}
}
