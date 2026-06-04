package dev.runtime_lab.flowit.domain.workspace.statemachine;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import dev.runtime_lab.flowit.domain.workspace.exception.DuplicateWorkspaceMemberException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceJoinReadyGuard extends WorkspaceJoinStateMachineSupport
	implements Guard<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Override
	public boolean evaluate(StateContext<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> context) {
		WorkspaceJoinRequest joinRequest = joinRequest(context);

		workspaceMemberRepository
			.findActiveByWorkspaceIdAndUserIdForUpdate(
				joinRequest.getWorkspace().getId(),
				joinRequest.getUser().getId()
			)
			.ifPresent(membership -> {
				context.getExtendedState().getVariables().put(
					WorkspaceJoinStateMachineHeaders.EXCEPTION,
					new DuplicateWorkspaceMemberException()
				);
			});

		return !context.getExtendedState().getVariables()
			.containsKey(WorkspaceJoinStateMachineHeaders.EXCEPTION);
	}
}
