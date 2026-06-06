package dev.runtime_lab.flowit.domain.user.service;

import dev.runtime_lab.flowit.domain.user.dto.UserMeResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserMeWorkspaceResponse;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceMembershipSummary;
import dev.runtime_lab.flowit.domain.workspace.service.internal.WorkspaceMembershipQueryService;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserMeService {

	private final CurrentUserProvider currentUserProvider;
	private final WorkspaceMembershipQueryService workspaceMembershipQueryService;

	@Transactional(readOnly = true)
	public UserMeResponse getMe(CurrentUser currentUser) {
		User requester = currentUserProvider.findActive(currentUser);
		List<UserMeWorkspaceResponse> workspaces = workspaceMembershipQueryService
			.findActiveMembershipSummaries(currentUser.id())
			.stream()
			.map(this::workspaceResponse)
			.toList();

		Long profileImageFileId = requester.getProfileImageFile() == null
			? null
			: requester.getProfileImageFile().getId();

		return new UserMeResponse(
			requester.getId(),
			requester.getEmail(),
			requester.getName(),
			requester.getStatus(),
			profileImageFileId,
			UserMeResponse.profileImageUrl(profileImageFileId),
			workspaces,
			List.of()
		);
	}

	@Transactional(readOnly = true)
	public List<UserMeWorkspaceResponse> getMeWorkspaces(CurrentUser currentUser) {
		currentUserProvider.findActive(currentUser);

		return workspaceMembershipQueryService.findActiveMembershipSummaries(currentUser.id())
			.stream()
			.map(this::workspaceResponse)
			.toList();
	}

	private UserMeWorkspaceResponse workspaceResponse(WorkspaceMembershipSummary summary) {
		return new UserMeWorkspaceResponse(
			summary.workspaceId(),
			summary.workspaceName(),
			summary.workspaceDescription(),
			summary.memberCount(),
			summary.role(),
			summary.joinedAt()
		);
	}
}
