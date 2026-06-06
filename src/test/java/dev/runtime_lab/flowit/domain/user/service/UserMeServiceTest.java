package dev.runtime_lab.flowit.domain.user.service;

import dev.runtime_lab.flowit.domain.user.dto.UserMeResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserMeWorkspaceResponse;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceMembershipSummary;
import dev.runtime_lab.flowit.domain.workspace.service.internal.WorkspaceMembershipQueryService;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserMeServiceTest {

	private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
	private final WorkspaceMembershipQueryService workspaceMembershipQueryService = mock(WorkspaceMembershipQueryService.class);
	private final UserMeService userMeService = new UserMeService(
		currentUserProvider,
		workspaceMembershipQueryService
	);

	@Test
	void getMeReturnsUserAndWorkspaceMemberships() {
		CurrentUser currentUser = new CurrentUser(1L, "claim@example.com", "claim-name");
		List<WorkspaceMembershipSummary> memberships = List.of(
			new WorkspaceMembershipSummary(10L, "Flowit", "Team workspace", 3L, WorkspaceMemberRole.OWNER, 2L)
		);

		when(currentUserProvider.findActive(currentUser)).thenReturn(activeUser());
		when(workspaceMembershipQueryService.findActiveMembershipSummaries(1L)).thenReturn(memberships);

		UserMeResponse response = userMeService.getMe(currentUser);

		assertEquals(1L, response.id());
		assertEquals("user@example.com", response.email());
		assertEquals("nickname", response.nickname());
		assertEquals(UserStatus.ACTIVE, response.status());
		assertEquals(null, response.profileImageFileId());
		assertEquals(null, response.profileImageUrl());
		assertEquals(10L, response.workspaces().get(0).id());
		assertEquals(3L, response.workspaces().get(0).memberCount());
		assertEquals(WorkspaceMemberRole.OWNER, response.workspaces().get(0).role());
		assertEquals(List.of(), response.notificationAlerts());
		verify(currentUserProvider).findActive(currentUser);
		verify(workspaceMembershipQueryService).findActiveMembershipSummaries(1L);
	}

	@Test
	void getMeRejectsMissingUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userMeService.getMe(currentUser)
		);
		verify(currentUserProvider).findActive(currentUser);
	}

	@Test
	void getMeWorkspacesReturnsCurrentUserWorkspaceMemberships() {
		List<WorkspaceMembershipSummary> memberships = List.of(
			new WorkspaceMembershipSummary(10L, "Flowit", "Team workspace", 3L, WorkspaceMemberRole.OWNER, 2L)
		);

		CurrentUser currentUser = new CurrentUser(1L, "claim@example.com", "claim-name");

		when(currentUserProvider.findActive(currentUser)).thenReturn(activeUser());
		when(workspaceMembershipQueryService.findActiveMembershipSummaries(1L)).thenReturn(memberships);

		List<UserMeWorkspaceResponse> response = userMeService.getMeWorkspaces(currentUser);

		assertEquals(10L, response.get(0).id());
		assertEquals(3L, response.get(0).memberCount());
		assertEquals(WorkspaceMemberRole.OWNER, response.get(0).role());
		verify(currentUserProvider).findActive(currentUser);
		verify(workspaceMembershipQueryService).findActiveMembershipSummaries(1L);
	}

	@Test
	void getMeWorkspacesRejectsMissingUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");

		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userMeService.getMeWorkspaces(currentUser)
		);
		verify(currentUserProvider).findActive(currentUser);
	}

	@Test
	void getMeWorkspacesRejectsInactiveUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");

		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userMeService.getMeWorkspaces(currentUser)
		);
		verify(currentUserProvider).findActive(currentUser);
	}

	private User activeUser() {
		return User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.status(UserStatus.ACTIVE)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
