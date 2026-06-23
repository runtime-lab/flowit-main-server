package dev.runtime_lab.flowit.global.dev;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

class LocalDevScenarioContext {

	private final User owner;
	private final CurrentUser currentUser;
	private final Workspace workspace;
	private final WorkspaceMember ownerMembership;
	private final Map<String, Long> taskIdsByRecordKey = new LinkedHashMap<>();

	LocalDevScenarioContext(
		User owner,
		CurrentUser currentUser,
		Workspace workspace,
		WorkspaceMember ownerMembership
	) {
		this.owner = owner;
		this.currentUser = currentUser;
		this.workspace = workspace;
		this.ownerMembership = ownerMembership;
	}

	User owner() {
		return owner;
	}

	CurrentUser currentUser() {
		return currentUser;
	}

	Workspace workspace() {
		return workspace;
	}

	WorkspaceMember ownerMembership() {
		return ownerMembership;
	}

	void putTaskId(String recordKey, Long taskId) {
		taskIdsByRecordKey.put(recordKey, taskId);
	}

	Optional<Long> taskId(String recordKey) {
		return Optional.ofNullable(taskIdsByRecordKey.get(recordKey));
	}
}
