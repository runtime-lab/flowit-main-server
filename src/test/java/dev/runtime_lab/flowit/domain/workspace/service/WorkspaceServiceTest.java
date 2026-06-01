package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceInviteCodeGenerationException;
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

	private final UserRepository userRepository = mock(UserRepository.class);
	private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
	private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
	private final WorkspaceInviteCodeGenerator workspaceInviteCodeGenerator = mock(WorkspaceInviteCodeGenerator.class);
	private final Clock clock = Clock.fixed(Instant.ofEpochSecond(1779889000L), ZoneOffset.UTC);
	private final WorkspaceService workspaceService = new WorkspaceService(
		userRepository,
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

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(creator));
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

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(creator));
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

		when(userRepository.findActiveById(1L)).thenReturn(Optional.empty());

		assertThrows(InvalidAuthenticatedUserException.class, () -> workspaceService.create(currentUser, request));
		verify(workspaceRepository, never()).save(org.mockito.ArgumentMatchers.any(Workspace.class));
		verify(workspaceMemberRepository, never()).save(org.mockito.ArgumentMatchers.any(WorkspaceMember.class));
	}

	@Test
	void createRejectsInactiveUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		WorkspaceCreateRequest request = new WorkspaceCreateRequest("Flowit", null);
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.status(UserStatus.LOCKED)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

		assertThrows(InvalidAuthenticatedUserException.class, () -> workspaceService.create(currentUser, request));
		verify(workspaceRepository, never()).save(org.mockito.ArgumentMatchers.any(Workspace.class));
		verify(workspaceMemberRepository, never()).save(org.mockito.ArgumentMatchers.any(WorkspaceMember.class));
	}

	@Test
	void createThrowsWhenInviteCodeGenerationAttemptsAreExhausted() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		WorkspaceCreateRequest request = new WorkspaceCreateRequest("Flowit", null);

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(activeUser()));
		when(workspaceInviteCodeGenerator.generate()).thenReturn("DUPL-CODE-0001");
		when(workspaceRepository.existsByInviteCode("DUPL-CODE-0001")).thenReturn(true);

		assertThrows(WorkspaceInviteCodeGenerationException.class, () -> workspaceService.create(currentUser, request));
		verify(workspaceRepository, never()).save(org.mockito.ArgumentMatchers.any(Workspace.class));
		verify(workspaceMemberRepository, never()).save(org.mockito.ArgumentMatchers.any(WorkspaceMember.class));
	}

	@Test
	void deleteSoftDeletesWorkspaceAndActiveMembersWhenCurrentUserIsOwner() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		User user = activeUser();
		Workspace workspace = workspace();

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));
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

		when(userRepository.findActiveById(1L)).thenReturn(Optional.empty());

		assertThrows(InvalidAuthenticatedUserException.class, () -> workspaceService.delete(currentUser, 10L));
		verify(workspaceRepository, never()).findActiveByIdForUpdate(10L);
		verify(workspaceMemberRepository, never()).softDeleteActiveByWorkspaceId(10L, 1779889000L);
	}

	@Test
	void deleteRejectsInactiveUser() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		User user = User.builder()
			.id(1L)
			.email("user@example.com")
			.passwordHash("hash")
			.name("nickname")
			.status(UserStatus.LOCKED)
			.createdAt(1L)
			.updatedAt(1L)
			.build();

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

		assertThrows(InvalidAuthenticatedUserException.class, () -> workspaceService.delete(currentUser, 10L));
		verify(workspaceRepository, never()).findActiveByIdForUpdate(10L);
		verify(workspaceMemberRepository, never()).softDeleteActiveByWorkspaceId(10L, 1779889000L);
	}

	@Test
	void deleteRejectsMissingWorkspace() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(activeUser()));
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.empty());

		assertThrows(WorkspaceNotFoundException.class, () -> workspaceService.delete(currentUser, 10L));
		verify(workspaceMemberRepository, never()).softDeleteActiveByWorkspaceId(10L, 1779889000L);
	}

	@Test
	void deleteRejectsNonOwnerMember() {
		CurrentUser currentUser = new CurrentUser(1L, "user@example.com", "nickname");
		User user = activeUser();
		Workspace workspace = workspace();

		when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));
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
