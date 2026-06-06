package dev.runtime_lab.flowit.domain.workspace.service.internal;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceAccessContext;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceAccessServiceTest {

	private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
	private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
	private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
	private final WorkspaceAccessService workspaceAccessService = new WorkspaceAccessService(
		currentUserProvider,
		workspaceRepository,
		workspaceMemberRepository
	);

	@Test
	void resolveMemberAccessReturnsRequesterWorkspaceAndMembership() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "Member");
		User requester = user(1L);
		Workspace workspace = workspace(requester);
		WorkspaceMember membership = workspaceMember(10L, workspace, requester);

		when(currentUserProvider.findActive(currentUser)).thenReturn(requester);
		when(workspaceRepository.findActiveById(100L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(100L, 1L))
			.thenReturn(Optional.of(membership));

		WorkspaceAccessContext context = workspaceAccessService.resolveMemberAccess(currentUser, 100L);

		assertEquals(requester, context.requester());
		assertEquals(workspace, context.workspace());
		assertEquals(membership, context.membership());
	}

	@Test
	void resolveMemberAccessRejectsMissingWorkspace() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "Member");
		User requester = user(1L);

		when(currentUserProvider.findActive(currentUser)).thenReturn(requester);
		when(workspaceRepository.findActiveById(100L)).thenReturn(Optional.empty());

		assertThrows(WorkspaceNotFoundException.class,
			() -> workspaceAccessService.resolveMemberAccess(currentUser, 100L));
	}

	@Test
	void resolveMemberAccessRejectsMissingMembership() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "Member");
		User requester = user(1L);
		Workspace workspace = workspace(requester);

		when(currentUserProvider.findActive(currentUser)).thenReturn(requester);
		when(workspaceRepository.findActiveById(100L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(100L, 1L))
			.thenReturn(Optional.empty());

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceAccessService.resolveMemberAccess(currentUser, 100L));
	}

	@Test
	void findActiveMemberDelegatesToRepository() {
		User requester = user(1L);
		Workspace workspace = workspace(requester);
		WorkspaceMember membership = workspaceMember(10L, workspace, requester);

		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberId(100L, 10L))
			.thenReturn(Optional.of(membership));

		Optional<WorkspaceMember> found = workspaceAccessService.findActiveMember(100L, 10L);

		assertEquals(Optional.of(membership), found);
		verify(workspaceMemberRepository).findActiveByWorkspaceIdAndMemberId(100L, 10L);
	}

	private User user(Long id) {
		return User.builder()
			.id(id)
			.email("user%s@example.com".formatted(id))
			.passwordHash("hash")
			.name("user%s".formatted(id))
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private Workspace workspace(User creator) {
		return Workspace.builder()
			.id(100L)
			.name("Flowit")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdBy(creator)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private WorkspaceMember workspaceMember(Long id, Workspace workspace, User user) {
		return WorkspaceMember.builder()
			.id(id)
			.workspace(workspace)
			.user(user)
			.role(WorkspaceMemberRole.MEMBER)
			.joinedAt(1L)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
