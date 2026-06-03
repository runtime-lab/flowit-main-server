package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceInviteCodeGenerationException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceServiceTest {

	private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
	private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
	private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
	private final WorkspaceInviteCodeGenerator workspaceInviteCodeGenerator = mock(WorkspaceInviteCodeGenerator.class);
	private final Clock clock = Clock.fixed(Instant.ofEpochSecond(1779889000L), ZoneOffset.UTC);
	private final WorkspaceService workspaceService = new WorkspaceService(
		currentUserProvider,
		workspaceRepository,
		workspaceMemberRepository,
		workspaceInviteCodeGenerator,
		clock
	);

	@Test
	void createCreatesWorkspaceAndOwnerMembership() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		WorkspaceCreateRequest request = new WorkspaceCreateRequest("Flowit", "Team workspace");
		User creator = activeUser();
		Workspace savedWorkspace = Workspace.builder()
			.id(10L)
			.name("Flowit")
			.description("Team workspace")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdBy(creator)
			.createdAt(1779889000L)
			.updatedAt(1779889000L)
			.build();
		ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
		ArgumentCaptor<WorkspaceMember> workspaceMemberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);

		when(currentUserProvider.findActive(currentUser)).thenReturn(creator);
		when(workspaceInviteCodeGenerator.generate()).thenReturn("A1B2-C3D4-E5F6");
		when(workspaceRepository.existsByInviteCode("A1B2-C3D4-E5F6")).thenReturn(false);
		when(workspaceRepository.save(workspaceCaptor.capture())).thenReturn(savedWorkspace);

		WorkspaceCreateResponse response = workspaceService.create(currentUser, request);

		Workspace workspaceToSave = workspaceCaptor.getValue();
		assertEquals("Flowit", workspaceToSave.getName());
		assertEquals("Team workspace", workspaceToSave.getDescription());
		assertEquals("A1B2-C3D4-E5F6", workspaceToSave.getInviteCode());
		assertSame(creator, workspaceToSave.getCreatedBy());
		assertEquals(1779889000L, workspaceToSave.getCreatedAt());
		assertEquals(1779889000L, workspaceToSave.getUpdatedAt());

		verify(workspaceMemberRepository).save(workspaceMemberCaptor.capture());
		WorkspaceMember workspaceMemberToSave = workspaceMemberCaptor.getValue();
		assertSame(savedWorkspace, workspaceMemberToSave.getWorkspace());
		assertSame(creator, workspaceMemberToSave.getUser());
		assertEquals(WorkspaceMemberRole.OWNER, workspaceMemberToSave.getRole());
		assertEquals(1779889000L, workspaceMemberToSave.getJoinedAt());
		assertEquals(1779889000L, workspaceMemberToSave.getCreatedAt());
		assertEquals(1779889000L, workspaceMemberToSave.getUpdatedAt());

		assertEquals(10L, response.id());
		assertEquals(1779889000L, response.createdAt());
	}

	@Test
	void createRetriesWhenInviteCodeAlreadyExists() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		WorkspaceCreateRequest request = new WorkspaceCreateRequest("Flowit", null);
		User creator = activeUser();
		Workspace savedWorkspace = Workspace.builder()
			.id(10L)
			.name("Flowit")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdBy(creator)
			.createdAt(1779889000L)
			.updatedAt(1779889000L)
			.build();
		ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);

		when(currentUserProvider.findActive(currentUser)).thenReturn(creator);
		when(workspaceInviteCodeGenerator.generate())
			.thenReturn("DUPL-CODE-0001")
			.thenReturn("A1B2-C3D4-E5F6");
		when(workspaceRepository.existsByInviteCode("DUPL-CODE-0001")).thenReturn(true);
		when(workspaceRepository.existsByInviteCode("A1B2-C3D4-E5F6")).thenReturn(false);
		when(workspaceRepository.save(workspaceCaptor.capture())).thenReturn(savedWorkspace);

		workspaceService.create(currentUser, request);

		assertEquals("A1B2-C3D4-E5F6", workspaceCaptor.getValue().getInviteCode());
	}

	@Test
	void createRejectsMissingUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		WorkspaceCreateRequest request = new WorkspaceCreateRequest("Flowit", null);

		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(InvalidAuthenticatedUserException.class, () -> workspaceService.create(currentUser, request));
		verify(workspaceRepository, never()).save(org.mockito.ArgumentMatchers.any(Workspace.class));
		verify(workspaceMemberRepository, never()).save(org.mockito.ArgumentMatchers.any(WorkspaceMember.class));
	}

	@Test
	void createRejectsInactiveUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		WorkspaceCreateRequest request = new WorkspaceCreateRequest("Flowit", null);
		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(InvalidAuthenticatedUserException.class, () -> workspaceService.create(currentUser, request));
		verify(workspaceRepository, never()).save(org.mockito.ArgumentMatchers.any(Workspace.class));
		verify(workspaceMemberRepository, never()).save(org.mockito.ArgumentMatchers.any(WorkspaceMember.class));
	}

	@Test
	void createThrowsWhenInviteCodeGenerationAttemptsAreExhausted() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		WorkspaceCreateRequest request = new WorkspaceCreateRequest("Flowit", null);

		when(currentUserProvider.findActive(currentUser)).thenReturn(activeUser());
		when(workspaceInviteCodeGenerator.generate()).thenReturn("DUPL-CODE-0001");
		when(workspaceRepository.existsByInviteCode("DUPL-CODE-0001")).thenReturn(true);

		assertThrows(WorkspaceInviteCodeGenerationException.class, () -> workspaceService.create(currentUser, request));
		verify(workspaceRepository, never()).save(org.mockito.ArgumentMatchers.any(Workspace.class));
		verify(workspaceMemberRepository, never()).save(org.mockito.ArgumentMatchers.any(WorkspaceMember.class));
	}

	@Test
	void getReturnsWorkspaceWhenCurrentUserIsMember() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		User user = activeUser();
		Workspace workspace = Workspace.builder()
			.id(10L)
			.name("Flowit")
			.description("Team workspace")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdBy(user)
			.createdAt(1779888000L)
			.updatedAt(1779889000L)
			.build();
		WorkspaceMember membership = WorkspaceMember.builder()
			.id(100L)
			.workspace(workspace)
			.user(user)
			.role(WorkspaceMemberRole.MEMBER)
			.joinedAt(1779888000L)
			.createdAt(1779888000L)
			.updatedAt(1779888000L)
			.build();

		when(currentUserProvider.findActive(currentUser)).thenReturn(user);
		when(workspaceRepository.findActiveById(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(10L, 1L))
			.thenReturn(Optional.of(membership));

		WorkspaceResponse response = workspaceService.get(currentUser, 10L);

		assertEquals(10L, response.id());
		assertEquals("Flowit", response.name());
		assertEquals("Team workspace", response.description());
		assertEquals("A1B2-C3D4-E5F6", response.inviteCode());
		assertEquals(1779888000L, response.createdAt());
		assertEquals(1779889000L, response.updatedAt());
	}

	@Test
	void getRejectsMissingUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");

		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(InvalidAuthenticatedUserException.class, () -> workspaceService.get(currentUser, 10L));
		verify(workspaceRepository, never()).findActiveById(10L);
		verify(workspaceMemberRepository, never()).findActiveByWorkspaceIdAndUserId(10L, 1L);
	}

	@Test
	void getRejectsMissingWorkspace() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");

		when(currentUserProvider.findActive(currentUser)).thenReturn(activeUser());
		when(workspaceRepository.findActiveById(10L)).thenReturn(Optional.empty());

		assertThrows(WorkspaceNotFoundException.class, () -> workspaceService.get(currentUser, 10L));
		verify(workspaceMemberRepository, never()).findActiveByWorkspaceIdAndUserId(10L, 1L);
	}

	@Test
	void getRejectsNonMember() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		User user = activeUser();
		Workspace workspace = workspace();

		when(currentUserProvider.findActive(currentUser)).thenReturn(user);
		when(workspaceRepository.findActiveById(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(10L, 1L))
			.thenReturn(Optional.empty());

		WorkspaceMemberAccessDeniedException exception = assertThrows(
			WorkspaceMemberAccessDeniedException.class,
			() -> workspaceService.get(currentUser, 10L)
		);
		assertEquals("Workspace membership is required.", exception.getMessage());
	}

	@Test
	void deleteSoftDeletesWorkspaceAndActiveMembersWhenCurrentUserIsOwner() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		User user = activeUser();
		Workspace workspace = workspace();

		when(currentUserProvider.findActive(currentUser)).thenReturn(user);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.existsActiveOwnerByWorkspaceAndUser(workspace, user)).thenReturn(true);

		workspaceService.delete(currentUser, 10L);

		assertEquals(1779889000L, workspace.getDeletedAt());
		assertEquals(1779889000L, workspace.getUpdatedAt());
		verify(workspaceMemberRepository).softDeleteActiveByWorkspaceId(10L, 1779889000L);
	}

	@Test
	void deleteRejectsMissingUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");

		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(InvalidAuthenticatedUserException.class, () -> workspaceService.delete(currentUser, 10L));
		verify(workspaceRepository, never()).findActiveByIdForUpdate(10L);
		verify(workspaceMemberRepository, never()).softDeleteActiveByWorkspaceId(10L, 1779889000L);
	}

	@Test
	void deleteRejectsInactiveUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(InvalidAuthenticatedUserException.class, () -> workspaceService.delete(currentUser, 10L));
		verify(workspaceRepository, never()).findActiveByIdForUpdate(10L);
		verify(workspaceMemberRepository, never()).softDeleteActiveByWorkspaceId(10L, 1779889000L);
	}

	@Test
	void deleteRejectsMissingWorkspace() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");

		when(currentUserProvider.findActive(currentUser)).thenReturn(activeUser());
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.empty());

		assertThrows(WorkspaceNotFoundException.class, () -> workspaceService.delete(currentUser, 10L));
		verify(workspaceMemberRepository, never()).softDeleteActiveByWorkspaceId(10L, 1779889000L);
	}

	@Test
	void deleteRejectsNonOwnerMember() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		User user = activeUser();
		Workspace workspace = workspace();

		when(currentUserProvider.findActive(currentUser)).thenReturn(user);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.existsActiveOwnerByWorkspaceAndUser(workspace, user)).thenReturn(false);

		assertThrows(WorkspaceAccessDeniedException.class, () -> workspaceService.delete(currentUser, 10L));
		assertNull(workspace.getDeletedAt());
		verify(workspaceMemberRepository, never()).softDeleteActiveByWorkspaceId(10L, 1779889000L);
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

	private Workspace workspace() {
		return Workspace.builder()
			.id(10L)
			.name("Flowit")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdBy(activeUser())
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
