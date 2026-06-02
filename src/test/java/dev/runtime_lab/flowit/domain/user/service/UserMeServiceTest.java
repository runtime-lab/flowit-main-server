package dev.runtime_lab.flowit.domain.user.service;

import dev.runtime_lab.flowit.domain.user.dto.UserMeResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserMeWorkspaceResponse;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserMeServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
	private final UserMeService userMeService = new UserMeService(userRepository, workspaceMemberRepository);

	@Test
	void getMeReturnsUserAndWorkspaceMemberships() {
		UserMeResponse expected = new UserMeResponse(
			1L,
			"user@example.com",
			"nickname",
			UserStatus.ACTIVE,
			null,
			null,
			List.of(new UserMeWorkspaceResponse(10L, "Flowit", "Team workspace", 3L, WorkspaceMemberRole.OWNER, 2L)),
			List.of()
		);

		when(userRepository.findActiveMeById(1L)).thenReturn(Optional.of(expected));

		UserMeResponse response = userMeService.getMe(new CurrentUser(1L, "claim@example.com", "claim-name"));

		assertSame(expected, response);
		assertEquals(10L, response.workspaces().get(0).id());
		assertEquals(3L, response.workspaces().get(0).memberCount());
		assertEquals(WorkspaceMemberRole.OWNER, response.workspaces().get(0).role());
		assertEquals(List.of(), response.notificationAlerts());
		verify(userRepository).findActiveMeById(1L);
	}

	@Test
	void getMeRejectsMissingUser() {
		when(userRepository.findActiveMeById(1L)).thenReturn(Optional.empty());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userMeService.getMe(new CurrentUser(1L, "user@example.com", "nickname"))
		);
		verify(userRepository).findActiveMeById(1L);
	}

	@Test
	void getMeWorkspacesReturnsCurrentUserWorkspaceMemberships() {
		List<UserMeWorkspaceResponse> expected = List.of(
			new UserMeWorkspaceResponse(10L, "Flowit", "Team workspace", 3L, WorkspaceMemberRole.OWNER, 2L)
		);

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(activeUser()));
		when(workspaceMemberRepository.findActiveUserWorkspaces(1L)).thenReturn(expected);

		List<UserMeWorkspaceResponse> response = userMeService.getMeWorkspaces(
			new CurrentUser(1L, "claim@example.com", "claim-name")
		);

		assertSame(expected, response);
		assertEquals(10L, response.get(0).id());
		assertEquals(3L, response.get(0).memberCount());
		assertEquals(WorkspaceMemberRole.OWNER, response.get(0).role());
		verify(userRepository).findActiveById(1L);
		verify(workspaceMemberRepository).findActiveUserWorkspaces(1L);
	}

	@Test
	void getMeWorkspacesRejectsMissingUser() {
		when(userRepository.findActiveById(1L)).thenReturn(Optional.empty());

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userMeService.getMeWorkspaces(new CurrentUser(1L, "user@example.com", "nickname"))
		);
		verify(userRepository).findActiveById(1L);
	}

	@Test
	void getMeWorkspacesRejectsInactiveUser() {
		User lockedUser = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.status(UserStatus.LOCKED)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(lockedUser));

		assertThrows(
			InvalidAuthenticatedUserException.class,
			() -> userMeService.getMeWorkspaces(new CurrentUser(1L, "user@example.com", "nickname"))
		);
		verify(userRepository).findActiveById(1L);
	}

	private User activeUser() {
		return User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
