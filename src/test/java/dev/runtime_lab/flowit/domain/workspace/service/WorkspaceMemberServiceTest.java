package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.service.internal.CurrentUserProvider;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberRoleUpdateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMemberResponse;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceMembersResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberAccessDeniedException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceMemberNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceNotFoundException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.authentication.InvalidAuthenticatedUserException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceMemberServiceTest {

	private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
	private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
	private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
	private final Clock clock = Clock.fixed(Instant.ofEpochSecond(1779889000L), ZoneOffset.UTC);
	private final WorkspaceMemberService workspaceMemberService = new WorkspaceMemberService(
		currentUserProvider,
		workspaceRepository,
		workspaceMemberRepository,
		clock
	);

	@Test
	void membersReturnsInviteCodeAndWorkspaceMembersWhenRequesterIsMember() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		User requester = activeUser(1L);
		Workspace workspace = workspace(requester);
		WorkspaceMember requesterMembership = workspaceMember(100L, workspace, requester, WorkspaceMemberRole.MEMBER);
		List<WorkspaceMemberResponse> members = List.of(
			new WorkspaceMemberResponse(
				200L,
				"Owner",
				"owner@example.com",
				UserStatus.ACTIVE,
				WorkspaceMemberRole.OWNER
			),
			new WorkspaceMemberResponse(
				201L,
				"Member",
				"member@example.com",
				UserStatus.ACTIVE,
				WorkspaceMemberRole.MEMBER
			)
		);

		when(currentUserProvider.findActive(currentUser)).thenReturn(requester);
		when(workspaceRepository.findActiveById(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(10L, 1L))
			.thenReturn(Optional.of(requesterMembership));
		when(workspaceMemberRepository.findActiveMembersByWorkspaceId(10L)).thenReturn(members);

		WorkspaceMembersResponse response = workspaceMemberService.members(currentUser, 10L);

		assertEquals("A1B2-C3D4-E5F6", response.inviteCode());
		assertEquals(members, response.members());
	}

	@Test
	void membersRejectsMissingWorkspace() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		User requester = activeUser(1L);

		when(currentUserProvider.findActive(currentUser)).thenReturn(requester);
		when(workspaceRepository.findActiveById(10L)).thenReturn(Optional.empty());

		assertThrows(WorkspaceNotFoundException.class,
			() -> workspaceMemberService.members(currentUser, 10L));
		verify(workspaceMemberRepository, never()).findActiveByWorkspaceIdAndUserId(10L, 1L);
	}

	@Test
	void membersRejectsRequesterOutsideWorkspace() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		User requester = activeUser(1L);
		Workspace workspace = workspace(activeUser(2L));

		when(currentUserProvider.findActive(currentUser)).thenReturn(requester);
		when(workspaceRepository.findActiveById(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(10L, 1L))
			.thenReturn(Optional.empty());

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceMemberService.members(currentUser, 10L));
		verify(workspaceMemberRepository, never()).findActiveMembersByWorkspaceId(10L);
	}

	@Test
	void membersRejectsMissingUser() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");

		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(InvalidAuthenticatedUserException.class,
			() -> workspaceMemberService.members(currentUser, 10L));
		verify(workspaceRepository, never()).findActiveById(10L);
	}

	@Test
	void membersRejectsInactiveUser() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(InvalidAuthenticatedUserException.class,
			() -> workspaceMemberService.members(currentUser, 10L));
		verify(workspaceRepository, never()).findActiveById(10L);
	}

	@Test
	void updateRoleAllowsOwnerToChangeMemberRole() {
		CurrentUser currentUser = new CurrentUser(1L, "owner@example.com", "owner");
		User owner = activeUser(1L);
		Workspace workspace = workspace(owner);
		WorkspaceMember ownerMembership = workspaceMember(100L, workspace, owner, WorkspaceMemberRole.OWNER);
		WorkspaceMember targetMembership = workspaceMember(200L, workspace, activeUser(2L), WorkspaceMemberRole.MEMBER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(owner);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(ownerMembership));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L))
			.thenReturn(Optional.of(targetMembership));

		workspaceMemberService.updateRole(
			currentUser,
			10L,
			200L,
			new WorkspaceMemberRoleUpdateRequest(WorkspaceMemberRole.ADMIN)
		);

		assertEquals(WorkspaceMemberRole.ADMIN, targetMembership.getRole());
		assertEquals(1779889000L, targetMembership.getUpdatedAt());
		verify(workspaceMemberRepository, never()).countActiveOwnersByWorkspaceId(10L);
	}

	@Test
	void updateRoleAllowsAdminToChangeMemberRoleExceptOwner() {
		CurrentUser currentUser = new CurrentUser(1L, "admin@example.com", "admin");
		User admin = activeUser(1L);
		Workspace workspace = workspace(admin);
		WorkspaceMember adminMembership = workspaceMember(100L, workspace, admin, WorkspaceMemberRole.ADMIN);
		WorkspaceMember targetMembership = workspaceMember(200L, workspace, activeUser(2L), WorkspaceMemberRole.MEMBER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(admin);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(adminMembership));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L))
			.thenReturn(Optional.of(targetMembership));

		workspaceMemberService.updateRole(
			currentUser,
			10L,
			200L,
			new WorkspaceMemberRoleUpdateRequest(WorkspaceMemberRole.ADMIN)
		);

		assertEquals(WorkspaceMemberRole.ADMIN, targetMembership.getRole());
		assertEquals(1779889000L, targetMembership.getUpdatedAt());
	}

	@Test
	void updateRoleRejectsAdminPromotingMemberToOwner() {
		CurrentUser currentUser = new CurrentUser(1L, "admin@example.com", "admin");
		User admin = activeUser(1L);
		Workspace workspace = workspace(admin);
		WorkspaceMember adminMembership = workspaceMember(100L, workspace, admin, WorkspaceMemberRole.ADMIN);
		WorkspaceMember targetMembership = workspaceMember(200L, workspace, activeUser(2L), WorkspaceMemberRole.MEMBER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(admin);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(adminMembership));

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceMemberService.updateRole(
				currentUser,
				10L,
				200L,
				new WorkspaceMemberRoleUpdateRequest(WorkspaceMemberRole.OWNER)
			));
		assertEquals(WorkspaceMemberRole.MEMBER, targetMembership.getRole());
		assertEquals(1L, targetMembership.getUpdatedAt());
		verify(workspaceMemberRepository, never()).findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L);
	}

	@Test
	void updateRoleAllowsOwnerDemotionWhenAnotherOwnerExists() {
		CurrentUser currentUser = new CurrentUser(1L, "owner@example.com", "owner");
		User owner = activeUser(1L);
		Workspace workspace = workspace(owner);
		WorkspaceMember requesterMembership = workspaceMember(100L, workspace, owner, WorkspaceMemberRole.OWNER);
		WorkspaceMember targetMembership = workspaceMember(200L, workspace, activeUser(2L), WorkspaceMemberRole.OWNER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(owner);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(requesterMembership));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L))
			.thenReturn(Optional.of(targetMembership));
		when(workspaceMemberRepository.countActiveOwnersByWorkspaceId(10L)).thenReturn(2L);

		workspaceMemberService.updateRole(
			currentUser,
			10L,
			200L,
			new WorkspaceMemberRoleUpdateRequest(WorkspaceMemberRole.ADMIN)
		);

		assertEquals(WorkspaceMemberRole.ADMIN, targetMembership.getRole());
		assertEquals(1779889000L, targetMembership.getUpdatedAt());
	}

	@Test
	void updateRoleRejectsOnlyOwnerDemotion() {
		CurrentUser currentUser = new CurrentUser(1L, "owner@example.com", "owner");
		User owner = activeUser(1L);
		Workspace workspace = workspace(owner);
		WorkspaceMember ownerMembership = workspaceMember(100L, workspace, owner, WorkspaceMemberRole.OWNER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(owner);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(ownerMembership));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 100L))
			.thenReturn(Optional.of(ownerMembership));
		when(workspaceMemberRepository.countActiveOwnersByWorkspaceId(10L)).thenReturn(1L);

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceMemberService.updateRole(
				currentUser,
				10L,
				100L,
				new WorkspaceMemberRoleUpdateRequest(WorkspaceMemberRole.ADMIN)
			));
		assertEquals(WorkspaceMemberRole.OWNER, ownerMembership.getRole());
		assertEquals(1L, ownerMembership.getUpdatedAt());
	}

	@Test
	void updateRoleRejectsMemberRequester() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		User member = activeUser(1L);
		Workspace workspace = workspace(member);
		WorkspaceMember requesterMembership = workspaceMember(100L, workspace, member, WorkspaceMemberRole.MEMBER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(member);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(requesterMembership));

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceMemberService.updateRole(
				currentUser,
				10L,
				200L,
				new WorkspaceMemberRoleUpdateRequest(WorkspaceMemberRole.ADMIN)
			));
		verify(workspaceMemberRepository, never()).findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L);
	}

	@Test
	void removeAllowsOwnerToRemoveAdmin() {
		CurrentUser currentUser = new CurrentUser(1L, "owner@example.com", "owner");
		User owner = activeUser(1L);
		Workspace workspace = workspace(owner);
		WorkspaceMember ownerMembership = workspaceMember(100L, workspace, owner, WorkspaceMemberRole.OWNER);
		WorkspaceMember targetMembership = workspaceMember(200L, workspace, activeUser(2L), WorkspaceMemberRole.ADMIN);

		when(currentUserProvider.findActive(currentUser)).thenReturn(owner);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(ownerMembership));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L))
			.thenReturn(Optional.of(targetMembership));

		workspaceMemberService.remove(currentUser, 10L, 200L);

		assertEquals(1779889000L, targetMembership.getDeletedAt());
		assertEquals(1779889000L, targetMembership.getUpdatedAt());
	}

	@Test
	void removeAllowsAdminToRemoveMember() {
		CurrentUser currentUser = new CurrentUser(1L, "admin@example.com", "admin");
		User admin = activeUser(1L);
		Workspace workspace = workspace(admin);
		WorkspaceMember adminMembership = workspaceMember(100L, workspace, admin, WorkspaceMemberRole.ADMIN);
		WorkspaceMember targetMembership = workspaceMember(200L, workspace, activeUser(2L), WorkspaceMemberRole.MEMBER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(admin);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(adminMembership));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L))
			.thenReturn(Optional.of(targetMembership));

		workspaceMemberService.remove(currentUser, 10L, 200L);

		assertEquals(1779889000L, targetMembership.getDeletedAt());
		assertEquals(1779889000L, targetMembership.getUpdatedAt());
	}

	@Test
	void removeRejectsSelfRemoval() {
		CurrentUser currentUser = new CurrentUser(1L, "owner@example.com", "owner");
		User owner = activeUser(1L);
		Workspace workspace = workspace(owner);
		WorkspaceMember ownerMembership = workspaceMember(100L, workspace, owner, WorkspaceMemberRole.OWNER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(owner);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(ownerMembership));

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceMemberService.remove(currentUser, 10L, 100L));
		verify(workspaceMemberRepository, never()).findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 100L);
	}

	@Test
	void removeRejectsMemberRequester() {
		CurrentUser currentUser = new CurrentUser(1L, "member@example.com", "member");
		User member = activeUser(1L);
		Workspace workspace = workspace(member);
		WorkspaceMember requesterMembership = workspaceMember(100L, workspace, member, WorkspaceMemberRole.MEMBER);
		WorkspaceMember targetMembership = workspaceMember(200L, workspace, activeUser(2L), WorkspaceMemberRole.MEMBER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(member);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(requesterMembership));

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceMemberService.remove(currentUser, 10L, 200L));
		assertNull(targetMembership.getDeletedAt());
		verify(workspaceMemberRepository, never()).findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L);
	}

	@Test
	void removeRejectsOwnerTarget() {
		CurrentUser currentUser = new CurrentUser(1L, "admin@example.com", "admin");
		User admin = activeUser(1L);
		Workspace workspace = workspace(admin);
		WorkspaceMember adminMembership = workspaceMember(100L, workspace, admin, WorkspaceMemberRole.ADMIN);
		WorkspaceMember ownerMembership = workspaceMember(200L, workspace, activeUser(2L), WorkspaceMemberRole.OWNER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(admin);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(adminMembership));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L))
			.thenReturn(Optional.of(ownerMembership));

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceMemberService.remove(currentUser, 10L, 200L));
		assertNull(ownerMembership.getDeletedAt());
	}

	@Test
	void removeRejectsOwnerTargetWhenRequesterIsOwner() {
		CurrentUser currentUser = new CurrentUser(1L, "owner@example.com", "owner");
		User owner = activeUser(1L);
		Workspace workspace = workspace(owner);
		WorkspaceMember requesterMembership = workspaceMember(100L, workspace, owner, WorkspaceMemberRole.OWNER);
		WorkspaceMember targetOwnerMembership = workspaceMember(200L, workspace, activeUser(2L), WorkspaceMemberRole.OWNER);

		when(currentUserProvider.findActive(currentUser)).thenReturn(owner);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(requesterMembership));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L))
			.thenReturn(Optional.of(targetOwnerMembership));

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceMemberService.remove(currentUser, 10L, 200L));
		assertNull(targetOwnerMembership.getDeletedAt());
	}

	@Test
	void removeRejectsMissingRequesterMembership() {
		CurrentUser currentUser = new CurrentUser(1L, "admin@example.com", "admin");
		User admin = activeUser(1L);
		Workspace workspace = workspace(admin);

		when(currentUserProvider.findActive(currentUser)).thenReturn(admin);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.empty());

		assertThrows(WorkspaceMemberAccessDeniedException.class,
			() -> workspaceMemberService.remove(currentUser, 10L, 200L));
	}

	@Test
	void removeRejectsMissingTargetMembership() {
		CurrentUser currentUser = new CurrentUser(1L, "admin@example.com", "admin");
		User admin = activeUser(1L);
		Workspace workspace = workspace(admin);
		WorkspaceMember adminMembership = workspaceMember(100L, workspace, admin, WorkspaceMemberRole.ADMIN);

		when(currentUserProvider.findActive(currentUser)).thenReturn(admin);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndUserIdForUpdate(10L, 1L))
			.thenReturn(Optional.of(adminMembership));
		when(workspaceMemberRepository.findActiveByWorkspaceIdAndMemberIdForUpdate(10L, 200L))
			.thenReturn(Optional.empty());

		assertThrows(WorkspaceMemberNotFoundException.class,
			() -> workspaceMemberService.remove(currentUser, 10L, 200L));
	}

	@Test
	void removeRejectsMissingWorkspace() {
		CurrentUser currentUser = new CurrentUser(1L, "admin@example.com", "admin");
		User admin = activeUser(1L);

		when(currentUserProvider.findActive(currentUser)).thenReturn(admin);
		when(workspaceRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.empty());

		assertThrows(WorkspaceNotFoundException.class,
			() -> workspaceMemberService.remove(currentUser, 10L, 200L));
	}

	@Test
	void removeRejectsMissingUser() {
		CurrentUser currentUser = new CurrentUser(1L, "admin@example.com", "admin");

		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(InvalidAuthenticatedUserException.class,
			() -> workspaceMemberService.remove(currentUser, 10L, 200L));
	}

	@Test
	void removeRejectsInactiveUser() {
		CurrentUser currentUser = new CurrentUser(1L, "admin@example.com", "admin");
		when(currentUserProvider.findActive(currentUser)).thenThrow(new InvalidAuthenticatedUserException());

		assertThrows(InvalidAuthenticatedUserException.class,
			() -> workspaceMemberService.remove(currentUser, 10L, 200L));
	}

	private User activeUser(Long id) {
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
			.id(10L)
			.name("Flowit")
			.inviteCode("A1B2-C3D4-E5F6")
			.createdBy(creator)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}

	private WorkspaceMember workspaceMember(
		Long id,
		Workspace workspace,
		User user,
		WorkspaceMemberRole role
	) {
		return WorkspaceMember.builder()
			.id(id)
			.workspace(workspace)
			.user(user)
			.role(role)
			.joinedAt(1L)
			.createdAt(1L)
			.updatedAt(1L)
			.build();
	}
}
