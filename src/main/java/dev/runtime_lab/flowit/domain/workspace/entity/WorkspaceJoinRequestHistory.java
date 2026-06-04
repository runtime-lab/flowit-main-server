package dev.runtime_lab.flowit.domain.workspace.entity;

import dev.runtime_lab.flowit.domain.user.entity.User;
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
	name = "workspace_join_request_histories",
	indexes = {
		@Index(name = "idx_workspace_join_request_histories_request_id", columnList = "join_request_id"),
		@Index(name = "idx_workspace_join_request_histories_changed_at", columnList = "changed_at")
	}
)
public class WorkspaceJoinRequestHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "join_request_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_workspace_join_request_histories_request")
	)
	private WorkspaceJoinRequest joinRequest;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 30)
	private WorkspaceJoinRequestStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 30)
	private WorkspaceJoinRequestStatus toStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "event", nullable = false, length = 30)
	private WorkspaceJoinRequestEvent event;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "changed_by_user_id",
		foreignKey = @ForeignKey(name = "fk_workspace_join_request_histories_changed_by_user")
	)
	private User changedBy;

	@Column(name = "changed_at", nullable = false)
	private Long changedAt;
}
