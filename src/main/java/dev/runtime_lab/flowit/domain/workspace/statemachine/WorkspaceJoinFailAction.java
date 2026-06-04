package dev.runtime_lab.flowit.domain.workspace.statemachine;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceJoinRequestHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceJoinFailAction extends WorkspaceJoinStateMachineSupport
	implements Action<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> {

	private final WorkspaceJoinRequestHistoryRepository historyRepository;
	private final Clock clock;

	@Override
	public void execute(StateContext<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> context) {
		WorkspaceJoinRequest joinRequest = joinRequest(context);
		WorkspaceJoinRequestHistory history = joinRequest.fail(
			actor(context),
			now(),
			failureCode(context),
			failureMessage(context)
		);
		historyRepository.save(history);
	}

	private String failureCode(StateContext<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> context) {
		return (String) context.getMessageHeader(WorkspaceJoinStateMachineHeaders.FAILURE_CODE);
	}

	private String failureMessage(StateContext<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> context) {
		return (String) context.getMessageHeader(WorkspaceJoinStateMachineHeaders.FAILURE_MESSAGE);
	}

	private Long now() {
		return Instant.now(clock).getEpochSecond();
	}
}
