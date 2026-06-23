package dev.runtime_lab.flowit.global.dev;

import dev.runtime_lab.flowit.domain.user.dto.JoinRequest;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.user.repository.UserRepository;
import dev.runtime_lab.flowit.domain.user.service.UserJoinService;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateRequest;
import dev.runtime_lab.flowit.domain.workspace.dto.WorkspaceCreateResponse;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceRepository;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceService;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDevBaseScenarioProvisioner {

	private final UserJoinService userJoinService;
	private final UserRepository userRepository;
	private final WorkspaceService workspaceService;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final LocalDevSampleRecordRegistry sampleRecordRegistry;
	private final Clock clock;

	LocalDevScenarioContext reconcileBaseScenario() {
		User owner = ensureLocalOwner();
		CurrentUser currentUser = new CurrentUser(owner.getId(), owner.getEmail(), owner.getName());
		Workspace workspace = reconcileWorkspace(currentUser, owner);
		WorkspaceMember ownerMembership = findOwnerMembership(workspace, owner);

		return new LocalDevScenarioContext(owner, currentUser, workspace, ownerMembership);
	}

	private User ensureLocalOwner() {
		return userRepository.findActiveByEmail(LocalDevScenarioSamples.LOCAL_OWNER_EMAIL)
			.orElseGet(() -> {
				userJoinService.join(new JoinRequest(
					LocalDevScenarioSamples.LOCAL_OWNER_EMAIL,
					LocalDevScenarioSamples.LOCAL_OWNER_PASSWORD,
					LocalDevScenarioSamples.LOCAL_OWNER_NICKNAME
				));
				return findLocalOwner();
			});
	}

	private User findLocalOwner() {
		return userRepository.findActiveByEmail(LocalDevScenarioSamples.LOCAL_OWNER_EMAIL)
			.orElseThrow(() -> new IllegalStateException("Local development owner was not created."));
	}

	private Workspace reconcileWorkspace(CurrentUser currentUser, User owner) {
		Workspace workspace = sampleRecordRegistry
			.findEntityId(LocalDevScenarioSamples.WORKSPACE_RECORD_KEY, LocalDevSampleRecordType.WORKSPACE)
			.flatMap(workspaceRepository::findActiveById)
			.orElseGet(() -> findOrCreateWorkspace(currentUser, owner));

		reconcileWorkspaceFields(workspace);
		sampleRecordRegistry.upsert(
			LocalDevScenarioSamples.WORKSPACE_RECORD_KEY,
			LocalDevSampleRecordType.WORKSPACE,
			workspace.getId()
		);
		return workspace;
	}

	private Workspace findOrCreateWorkspace(CurrentUser currentUser, User owner) {
		return workspaceRepository.findActiveByCreatedByUserIdAndName(
				owner.getId(),
				LocalDevScenarioSamples.LOCAL_WORKSPACE_NAME
			)
			.orElseGet(() -> {
				WorkspaceCreateResponse response = workspaceService.create(
					currentUser,
					new WorkspaceCreateRequest(
						LocalDevScenarioSamples.LOCAL_WORKSPACE_NAME,
						LocalDevScenarioSamples.LOCAL_WORKSPACE_DESCRIPTION
					)
				);
				return workspaceRepository.findActiveById(response.id())
					.orElseThrow(() -> new IllegalStateException("Local development workspace was not created."));
			});
	}

	private void reconcileWorkspaceFields(Workspace workspace) {
		boolean changed = false;

		if (!Objects.equals(workspace.getName(), LocalDevScenarioSamples.LOCAL_WORKSPACE_NAME)) {
			workspace.setWorkspaceName(LocalDevScenarioSamples.LOCAL_WORKSPACE_NAME);
			changed = true;
		}

		if (!Objects.equals(workspace.getDescription(), LocalDevScenarioSamples.LOCAL_WORKSPACE_DESCRIPTION)) {
			workspace.setWorkspaceDescription(LocalDevScenarioSamples.LOCAL_WORKSPACE_DESCRIPTION);
			changed = true;
		}

		if (changed) {
			workspace.setUpdatedAt(Instant.now(clock).getEpochSecond());
		}
	}

	private WorkspaceMember findOwnerMembership(Workspace workspace, User owner) {
		return workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspace.getId(), owner.getId())
			.orElseThrow(() -> new IllegalStateException("Local development owner membership was not created."));
	}
}
