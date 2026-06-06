package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.activity.service.internal.WorkspaceActivityRecorder;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberRoleUpdateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMembersResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRemovalHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRoleHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberWithdrawalHistory;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRemovalHistoryRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRoleHistoryRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberWithdrawalHistoryRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.MEMBERSHIP_REQUIRED;
import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.OWNER_REQUIRED;
import static dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessMessages.ROLE_UPDATE_NOT_ALLOWED;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {

	private final CurrentUserProvider currentUserProvider;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberRoleHistoryRepository workspaceMemberRoleHistoryRepository;
	private final WorkspaceMemberRemovalHistoryRepository workspaceMemberRemovalHistoryRepository;
	private final WorkspaceMemberWithdrawalHistoryRepository workspaceMemberWithdrawalHistoryRepository;
	private final WorkspaceActivityRecorder workspaceActivityRecorder;
	private final Clock clock;

	@Transactional(readOnly = true)
	public WorkspaceMembersResponse members(CurrentUser currentUser, Long workspaceId) {
		User requester = currentUserProvider.findActive(currentUser);
		Workspace workspace = workspaceRepository.findActiveById(workspaceId)
			.orElseThrow(WorkspaceNotFoundException::new);

		workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspace.getId(), requester.getId())
			.orElseThrow(() -> new WorkspaceMemberAccessDeniedException(MEMBERSHIP_REQUIRED));

		List<WorkspaceMemberResponse> members = workspaceMemberRepository.findActiveMembersByWorkspaceId(workspace.getId());

		return new WorkspaceMembersResponse(workspace.getInviteCode(), members);
	}

	@Transactional
	public void updateRole(
		CurrentUser currentUser,
		Long workspaceId,
		Long memberId,
		WorkspaceMemberRoleUpdateRequest request
	) {
		User requester = currentUserProvider.findActive(currentUser);
		Workspace workspace = workspaceRepository.findActiveByIdForUpdate(workspaceId)
			.orElseThrow(WorkspaceNotFoundException::new);

		WorkspaceMember requesterMembership = workspaceMemberRepository
			.findActiveByWorkspaceIdAndUserIdForUpdate(workspace.getId(), requester.getId())
			.orElseThrow(() -> new WorkspaceMemberAccessDeniedException(ROLE_UPDATE_NOT_ALLOWED));

		WorkspaceMemberRole newRole = request.role();
		if (!requesterMembership.getRole().canUpdateMemberRoleTo(newRole)) {
			throw new WorkspaceMemberAccessDeniedException(ROLE_UPDATE_NOT_ALLOWED);
		}

		WorkspaceMember targetMembership = workspaceMemberRepository
			.findActiveByWorkspaceIdAndMemberIdForUpdate(workspace.getId(), memberId)
			.orElseThrow(WorkspaceMemberNotFoundException::new);

		if (targetMembership.getRole().isWorkspaceOwner() && !newRole.isWorkspaceOwner()) {
			long ownerCount = workspaceMemberRepository.countActiveOwnersByWorkspaceId(workspace.getId());
			if (ownerCount <= 1L) {
				throw new WorkspaceMemberAccessDeniedException(OWNER_REQUIRED);
			}
		}

		WorkspaceMemberRole previousRole = targetMembership.getRole();
		if (previousRole == newRole) {
			return;
		}

		Long now = Instant.now(clock).getEpochSecond();
		targetMembership.updateRole(newRole, now);

		WorkspaceMemberRoleHistory history = roleHistory(
			workspace,
			targetMembership,
			previousRole,
			newRole,
			requester,
			now
		);

		workspaceMemberRoleHistoryRepository.save(history);

		workspaceActivityRecorder.recordRoleChanged(
			workspace,
			requesterMembership,
			requester,
			targetMembership,
			previousRole,
			newRole,
			history,
			now
		);
	}

	@Transactional
	public void remove(CurrentUser currentUser, Long workspaceId, Long memberId) {
		User requester = currentUserProvider.findActive(currentUser);

		Workspace workspace = workspaceRepository.findActiveByIdForUpdate(workspaceId)
				.orElseThrow(WorkspaceNotFoundException::new);

		WorkspaceMember requesterMembership = workspaceMemberRepository
			.findActiveByWorkspaceIdAndUserIdForUpdate(workspace.getId(), requester.getId())
			.orElseThrow(WorkspaceMemberAccessDeniedException::new);

		if (!requesterMembership.getRole().canRemoveMember()) {
			throw new WorkspaceMemberAccessDeniedException();
		}

		if (requesterMembership.getId().equals(memberId)) {
			throw new WorkspaceMemberAccessDeniedException();
		}

		WorkspaceMember targetMembership = workspaceMemberRepository
			.findActiveByWorkspaceIdAndMemberIdForUpdate(workspace.getId(), memberId)
			.orElseThrow(WorkspaceMemberNotFoundException::new);

		if (targetMembership.getRole().isWorkspaceOwner()) {
			throw new WorkspaceMemberAccessDeniedException();
		}

		Long now = Instant.now(clock).getEpochSecond();
		WorkspaceMemberRole roleSnapshot = targetMembership.getRole();
		targetMembership.softDelete(now);

		WorkspaceMemberRemovalHistory history = WorkspaceMemberRemovalHistory.builder()
			.workspace(workspace)
			.workspaceMember(targetMembership)
			.targetUser(targetMembership.getUser())
			.roleSnapshot(roleSnapshot)
			.removedBy(requester)
			.removedAt(now)
			.build();

		workspaceMemberRemovalHistoryRepository.save(history);

		workspaceActivityRecorder.recordRemoved(
			workspace,
			requesterMembership,
			requester,
			targetMembership,
			roleSnapshot,
			history,
			now
		);
	}

	@Transactional
	public void withdraw(CurrentUser currentUser, Long workspaceId) {
		User requester = currentUserProvider.findActive(currentUser);

		Workspace workspace = workspaceRepository.findActiveByIdForUpdate(workspaceId)
			.orElseThrow(WorkspaceNotFoundException::new);

		WorkspaceMember requesterMembership = workspaceMemberRepository
			.findActiveByWorkspaceIdAndUserIdForUpdate(workspace.getId(), requester.getId())
			.orElseThrow(() -> new WorkspaceMemberAccessDeniedException(MEMBERSHIP_REQUIRED));

		Long now = Instant.now(clock).getEpochSecond();
		WorkspaceMemberRole roleSnapshot = requesterMembership.getRole();
		WorkspaceMember ownershipTransferredTo = null;

		if (roleSnapshot.isWorkspaceOwner()) {
			ownershipTransferredTo = workspaceMemberRepository
				.findOldestActiveAdminMemberIdByWorkspaceId(workspace.getId())
				.flatMap(memberId -> workspaceMemberRepository
					.findActiveByWorkspaceIdAndMemberIdForUpdate(workspace.getId(), memberId))
				.orElseThrow(() -> new WorkspaceMemberAccessDeniedException(OWNER_REQUIRED));
			if (!ownershipTransferredTo.getRole().isWorkspaceAdmin()) {
				throw new WorkspaceMemberAccessDeniedException(OWNER_REQUIRED);
			}
		}

		requesterMembership.softDelete(now);

		if (ownershipTransferredTo != null) {
			workspaceMemberRepository.flush();

			WorkspaceMemberRole previousRole = ownershipTransferredTo.getRole();
			ownershipTransferredTo.updateRole(WorkspaceMemberRole.OWNER, now);

			WorkspaceMemberRoleHistory roleHistory = roleHistory(
				workspace,
				ownershipTransferredTo,
				previousRole,
				WorkspaceMemberRole.OWNER,
				requester,
				now
			);

			workspaceMemberRoleHistoryRepository.save(roleHistory);

			workspaceActivityRecorder.recordRoleChanged(
				workspace,
				requesterMembership,
				requester,
				ownershipTransferredTo,
				previousRole,
				WorkspaceMemberRole.OWNER,
				roleHistory,
				now
			);
		}

		WorkspaceMemberWithdrawalHistory withdrawalHistory = WorkspaceMemberWithdrawalHistory.builder()
			.workspace(workspace)
			.workspaceMember(requesterMembership)
			.user(requester)
			.roleSnapshot(roleSnapshot)
			.ownershipTransferredToWorkspaceMember(ownershipTransferredTo)
			.ownershipTransferredToUser(ownershipTransferredTo == null ? null : ownershipTransferredTo.getUser())
			.withdrawnAt(now)
			.build();

		workspaceMemberWithdrawalHistoryRepository.save(withdrawalHistory);

		workspaceActivityRecorder.recordWithdrawn(
			workspace,
			requesterMembership,
			requester,
			roleSnapshot,
			ownershipTransferredTo,
			withdrawalHistory,
			now
		);
	}

	private WorkspaceMemberRoleHistory roleHistory(
		Workspace workspace,
		WorkspaceMember workspaceMember,
		WorkspaceMemberRole fromRole,
		WorkspaceMemberRole toRole,
		User changedBy,
		Long changedAt
	) {
		return WorkspaceMemberRoleHistory.builder()
			.workspace(workspace)
			.workspaceMember(workspaceMember)
			.fromRole(fromRole)
			.toRole(toRole)
			.changedBy(changedBy)
			.changedAt(changedAt)
			.build();
	}
}
