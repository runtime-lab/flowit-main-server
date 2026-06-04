package dev.runtime_lab.flowit.domain.workspace.statemachine;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import org.springframework.statemachine.StateContext;

abstract class WorkspaceJoinStateMachineSupport {

	protected WorkspaceJoinRequest joinRequest(
		StateContext<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> context
	) {
		return (WorkspaceJoinRequest) context.getMessageHeader(WorkspaceJoinStateMachineHeaders.JOIN_REQUEST);
	}

	protected User actor(StateContext<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> context) {
		return (User) context.getMessageHeader(WorkspaceJoinStateMachineHeaders.ACTOR);
	}
}
