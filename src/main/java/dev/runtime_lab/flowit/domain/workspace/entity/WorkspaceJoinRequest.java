package dev.runtime_lab.flowit.domain.workspace.entity;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceJoinRequestInvalidTransitionException;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
	name = "workspace_join_requests",
	indexes = {
		@Index(name = "idx_workspace_join_requests_workspace_id", columnList = "workspace_id"),
		@Index(name = "idx_workspace_join_requests_user_id", columnList = "user_id"),
		@Index(name = "idx_workspace_join_requests_status", columnList = "status"),
		@Index(name = "idx_workspace_join_requests_requested_at", columnList = "requested_at")
	}
)
public class WorkspaceJoinRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "workspace_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_workspace_join_requests_workspace")
	)
	private Workspace workspace;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "user_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_workspace_join_requests_user")
	)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "workspace_member_id",
		foreignKey = @ForeignKey(name = "fk_workspace_join_requests_workspace_member")
	)
	private WorkspaceMember workspaceMember;

	@Enumerated(EnumType.STRING)
	@Column(name = "method", nullable = false, length = 30)
	private WorkspaceJoinRequestMethod method;

	@Column(name = "invite_code_snapshot", nullable = false, length = 14)
	private String inviteCodeSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private WorkspaceJoinRequestStatus status;

	@Column(name = "requested_at", nullable = false)
	private Long requestedAt;

	@Column(name = "ready_at")
	private Long readyAt;

	@Column(name = "approved_at")
	private Long approvedAt;

	@Column(name = "joined_at")
	private Long joinedAt;

	@Column(name = "failed_at")
	private Long failedAt;

	@Column(name = "failure_code", length = 100)
	private String failureCode;

	@Column(name = "failure_message", length = 500)
	private String failureMessage;

	@Column(name = "created_at", nullable = false)
	private Long createdAt;

	@Column(name = "updated_at", nullable = false)
	private Long updatedAt;

	@Builder.Default
	@OrderBy("changedAt ASC, id ASC")
	@OneToMany(mappedBy = "joinRequest", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceJoinRequestHistory> histories = new ArrayList<>();

	public static WorkspaceJoinRequest inviteCode(Workspace workspace, User user, String inviteCode, Long now) {
		return WorkspaceJoinRequest.builder()
			.workspace(workspace)
			.user(user)
			.method(WorkspaceJoinRequestMethod.INVITE_CODE)
			.inviteCodeSnapshot(inviteCode)
			.status(WorkspaceJoinRequestStatus.PENDING)
			.requestedAt(now)
			.createdAt(now)
			.updatedAt(now)
			.build();
	}

	public WorkspaceJoinRequestHistory recordRequested(User actor, Long changedAt) {
		return addHistory(null, WorkspaceJoinRequestStatus.PENDING, WorkspaceJoinRequestEvent.REQUEST, actor, changedAt);
	}

	public WorkspaceJoinRequestHistory markReady(User actor, Long changedAt) {
		transition(WorkspaceJoinRequestStatus.READY, WorkspaceJoinRequestEvent.MARK_READY, changedAt);
		this.readyAt = changedAt;
		return addHistory(WorkspaceJoinRequestStatus.PENDING, WorkspaceJoinRequestStatus.READY,
			WorkspaceJoinRequestEvent.MARK_READY, actor, changedAt);
	}

	public WorkspaceJoinRequestHistory approve(User actor, Long changedAt) {
		transition(WorkspaceJoinRequestStatus.APPROVED, WorkspaceJoinRequestEvent.APPROVE, changedAt);
		this.approvedAt = changedAt;
		return addHistory(WorkspaceJoinRequestStatus.READY, WorkspaceJoinRequestStatus.APPROVED,
			WorkspaceJoinRequestEvent.APPROVE, actor, changedAt);
	}

	public WorkspaceJoinRequestHistory join(WorkspaceMember workspaceMember, User actor, Long changedAt) {
		transition(WorkspaceJoinRequestStatus.JOINED, WorkspaceJoinRequestEvent.JOIN, changedAt);
		this.workspaceMember = workspaceMember;
		this.joinedAt = changedAt;
		return addHistory(WorkspaceJoinRequestStatus.APPROVED, WorkspaceJoinRequestStatus.JOINED,
			WorkspaceJoinRequestEvent.JOIN, actor, changedAt);
	}

	public WorkspaceJoinRequestHistory fail(
		User actor,
		Long changedAt,
		String failureCode,
		String failureMessage
	) {
		WorkspaceJoinRequestStatus fromStatus = this.status;

		transition(WorkspaceJoinRequestStatus.FAILED, WorkspaceJoinRequestEvent.FAIL, changedAt);
		this.failedAt = changedAt;
		this.failureCode = failureCode;
		this.failureMessage = failureMessage;
		return addHistory(fromStatus, WorkspaceJoinRequestStatus.FAILED,
			WorkspaceJoinRequestEvent.FAIL, actor, changedAt);
	}

	private void transition(
		WorkspaceJoinRequestStatus targetStatus,
		WorkspaceJoinRequestEvent event,
		Long changedAt
	) {
		if (!canTransition(status, targetStatus, event)) {
			throw new WorkspaceJoinRequestInvalidTransitionException();
		}

		this.status = targetStatus;
		this.updatedAt = changedAt;
	}

	private boolean canTransition(
		WorkspaceJoinRequestStatus sourceStatus,
		WorkspaceJoinRequestStatus targetStatus,
		WorkspaceJoinRequestEvent event
	) {
		return switch (sourceStatus) {
			case PENDING -> (event == WorkspaceJoinRequestEvent.MARK_READY
				&& targetStatus == WorkspaceJoinRequestStatus.READY)
				|| isFailureTransition(targetStatus, event);
			case READY -> (event == WorkspaceJoinRequestEvent.APPROVE
				&& targetStatus == WorkspaceJoinRequestStatus.APPROVED)
				|| isFailureTransition(targetStatus, event);
			case APPROVED -> (event == WorkspaceJoinRequestEvent.JOIN
				&& targetStatus == WorkspaceJoinRequestStatus.JOINED)
				|| isFailureTransition(targetStatus, event);
			case JOINED, FAILED -> false;
		};
	}

	private boolean isFailureTransition(
		WorkspaceJoinRequestStatus targetStatus,
		WorkspaceJoinRequestEvent event
	) {
		return event == WorkspaceJoinRequestEvent.FAIL
			&& targetStatus == WorkspaceJoinRequestStatus.FAILED;
	}

	private WorkspaceJoinRequestHistory addHistory(
		WorkspaceJoinRequestStatus fromStatus,
		WorkspaceJoinRequestStatus toStatus,
		WorkspaceJoinRequestEvent event,
		User actor,
		Long changedAt
	) {
		WorkspaceJoinRequestHistory history = WorkspaceJoinRequestHistory.builder()
			.joinRequest(this)
			.fromStatus(fromStatus)
			.toStatus(toStatus)
			.event(event)
			.changedBy(actor)
			.changedAt(changedAt)
			.build();
		histories.add(history);

		return history;
	}
}
