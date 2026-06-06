package dev.runtime_lab.flowit.domain.activity.service.internal;

import dev.runtime_lab.flowit.domain.activity.dto.ActivityChangeElement;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityChangeResponse;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordAction;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordDomain;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityTargetType;
import dev.runtime_lab.flowit.domain.activity.entity.ActivityRecordSourceType;
import dev.runtime_lab.flowit.domain.activity.entity.WorkspaceActivityRecord;
import dev.runtime_lab.flowit.domain.activity.repository.WorkspaceActivityRecordRepository;
import dev.runtime_lab.flowit.domain.task.dto.TaskHistoryChangeResponse;
import dev.runtime_lab.flowit.domain.task.entity.TaskChangeHistory;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRemovalHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRoleHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberWithdrawalHistory;
import dev.runtime_lab.flowit.global.stereotype.InternalService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@InternalService
@RequiredArgsConstructor
public class WorkspaceActivityRecorder {

	private static final String ACTIVE_MEMBERSHIP_VALUE = "ACTIVE";

	private final WorkspaceActivityRecordRepository workspaceActivityRecordRepository;
	private final JsonMapper jsonMapper;

	public void recordTask(TaskChangeHistory history, List<TaskHistoryChangeResponse> changes) {
		workspaceActivityRecordRepository.save(WorkspaceActivityRecord.builder()
			.workspace(history.getWorkspace())
			.sourceType(ActivityRecordSourceType.TASK_CHANGE_HISTORY)
			.sourceId(history.getId())
			.domain(ActivityRecordDomain.TASK)
			.action(ActivityRecordAction.from(history.getAction()))
			.actorWorkspaceMember(history.getActorWorkspaceMember())
			.actorUser(history.getActorUser())
			.actorDisplayNameSnapshot(history.getActorDisplayNameSnapshot())
			.targetType(ActivityTargetType.TASK)
			.targetId(history.getTask().getId())
			.targetDisplayNameSnapshot(history.getTaskTitleSnapshot())
			.changesJson(writeChanges(taskActivityChanges(changes)))
			.occurredAt(history.getChangedAt())
			.build());
	}

	public void recordJoined(
		Workspace workspace,
		WorkspaceMember workspaceMember,
		User actorUser,
		WorkspaceJoinRequestHistory source,
		Long occurredAt
	) {
		workspaceActivityRecordRepository.save(memberActivityBuilder(
				workspace,
				ActivityRecordSourceType.WORKSPACE_JOIN_REQUEST_HISTORY,
				source.getId(),
				ActivityRecordAction.JOINED,
				workspaceMember,
				actorUser,
				workspaceMember,
				List.of(
					change(ActivityChangeElement.MEMBERSHIP, null, ActivityRecordAction.JOINED.name()),
					change(ActivityChangeElement.ROLE, null, workspaceMember.getRole().name())
				),
				occurredAt
			)
			.build());
	}

	public void recordRoleChanged(
		Workspace workspace,
		WorkspaceMember actorMember,
		User actorUser,
		WorkspaceMember targetMember,
		WorkspaceMemberRole fromRole,
		WorkspaceMemberRole toRole,
		WorkspaceMemberRoleHistory source,
		Long occurredAt
	) {
		workspaceActivityRecordRepository.save(memberActivityBuilder(
				workspace,
				ActivityRecordSourceType.WORKSPACE_MEMBER_ROLE_HISTORY,
				source.getId(),
				ActivityRecordAction.ROLE_CHANGED,
				actorMember,
				actorUser,
				targetMember,
				List.of(change(ActivityChangeElement.ROLE, fromRole.name(), toRole.name())),
				occurredAt
			)
			.build());
	}

	public void recordRemoved(
		Workspace workspace,
		WorkspaceMember actorMember,
		User actorUser,
		WorkspaceMember targetMember,
		WorkspaceMemberRole roleSnapshot,
		WorkspaceMemberRemovalHistory source,
		Long occurredAt
	) {
		workspaceActivityRecordRepository.save(memberActivityBuilder(
				workspace,
				ActivityRecordSourceType.WORKSPACE_MEMBER_REMOVAL_HISTORY,
				source.getId(),
				ActivityRecordAction.REMOVED,
				actorMember,
				actorUser,
				targetMember,
				List.of(
					change(ActivityChangeElement.MEMBERSHIP, ACTIVE_MEMBERSHIP_VALUE, ActivityRecordAction.REMOVED.name()),
					change(ActivityChangeElement.ROLE, roleSnapshot.name(), null)
				),
				occurredAt
			)
			.build());
	}

	public void recordWithdrawn(
		Workspace workspace,
		WorkspaceMember workspaceMember,
		User actorUser,
		WorkspaceMemberRole roleSnapshot,
		WorkspaceMember ownershipTransferredTo,
		WorkspaceMemberWithdrawalHistory source,
		Long occurredAt
	) {
		workspaceActivityRecordRepository.save(memberActivityBuilder(
				workspace,
				ActivityRecordSourceType.WORKSPACE_MEMBER_WITHDRAWAL_HISTORY,
				source.getId(),
				ActivityRecordAction.WITHDRAWN,
				workspaceMember,
				actorUser,
				workspaceMember,
				withdrawalChanges(roleSnapshot, ownershipTransferredTo),
				occurredAt
			)
			.build());
	}

	private List<ActivityChangeResponse> taskActivityChanges(List<TaskHistoryChangeResponse> changes) {
		return changes.stream()
			.map(change -> new ActivityChangeResponse(
				ActivityChangeElement.from(change.element()),
				change.from(),
				change.to()
			))
			.toList();
	}

	private List<ActivityChangeResponse> withdrawalChanges(
		WorkspaceMemberRole roleSnapshot,
		WorkspaceMember ownershipTransferredTo
	) {
		if (ownershipTransferredTo == null) {
			return List.of(
				change(ActivityChangeElement.MEMBERSHIP, ACTIVE_MEMBERSHIP_VALUE, ActivityRecordAction.WITHDRAWN.name()),
				change(ActivityChangeElement.ROLE, roleSnapshot.name(), null)
			);
		}

		return List.of(
			change(ActivityChangeElement.MEMBERSHIP, ACTIVE_MEMBERSHIP_VALUE, ActivityRecordAction.WITHDRAWN.name()),
			change(ActivityChangeElement.ROLE, roleSnapshot.name(), null),
			change(ActivityChangeElement.OWNERSHIP_TRANSFER, null, memberValue(ownershipTransferredTo))
		);
	}

	private WorkspaceActivityRecord.WorkspaceActivityRecordBuilder memberActivityBuilder(
		Workspace workspace,
		ActivityRecordSourceType sourceType,
		Long sourceId,
		ActivityRecordAction action,
		WorkspaceMember actorMember,
		User actorUser,
		WorkspaceMember targetMember,
		List<ActivityChangeResponse> changes,
		Long occurredAt
	) {
		return WorkspaceActivityRecord.builder()
			.workspace(workspace)
			.sourceType(sourceType)
			.sourceId(sourceId)
			.domain(ActivityRecordDomain.WORKSPACE_MEMBER)
			.action(action)
			.actorWorkspaceMember(actorMember)
			.actorUser(actorUser)
			.actorDisplayNameSnapshot(actorUser.getName())
			.targetType(ActivityTargetType.WORKSPACE_MEMBER)
			.targetId(targetMember.getId())
			.targetDisplayNameSnapshot(targetMember.getUser().getName())
			.changesJson(writeChanges(changes))
			.occurredAt(occurredAt);
	}

	private ActivityChangeResponse change(ActivityChangeElement element, Object from, Object to) {
		return new ActivityChangeResponse(element, from, to);
	}

	private Map<String, Object> memberValue(WorkspaceMember workspaceMember) {
		return Map.of(
			"memberId", workspaceMember.getId(),
			"userId", workspaceMember.getUser().getId(),
			"displayName", workspaceMember.getUser().getName()
		);
	}

	private String writeChanges(List<ActivityChangeResponse> changes) {
		try {
			return jsonMapper.writeValueAsString(changes);
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("Failed to serialize workspace activity changes.", exception);
		}
	}
}
