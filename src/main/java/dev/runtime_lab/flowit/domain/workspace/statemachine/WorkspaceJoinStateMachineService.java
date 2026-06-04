package dev.runtime_lab.flowit.domain.workspace.statemachine;

import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceJoinRequestInvalidTransitionException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.StateMachineEventResult.ResultType;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WorkspaceJoinStateMachineService {

	private static final String STATE_MACHINE_ID = "workspace-join-request-%d";

	private final StateMachineFactory<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> stateMachineFactory;

	public void send(WorkspaceJoinRequest joinRequest, WorkspaceJoinRequestEvent event, User actor) {
		Message<WorkspaceJoinRequestEvent> message = MessageBuilder
			.withPayload(event)
			.setHeader(WorkspaceJoinStateMachineHeaders.JOIN_REQUEST, joinRequest)
			.setHeader(WorkspaceJoinStateMachineHeaders.ACTOR, actor)
			.build();

		send(joinRequest, message);
	}

	public void sendFailure(
		WorkspaceJoinRequest joinRequest,
		User actor,
		String failureCode,
		String failureMessage
	) {
		Message<WorkspaceJoinRequestEvent> message = MessageBuilder
			.withPayload(WorkspaceJoinRequestEvent.FAIL)
			.setHeader(WorkspaceJoinStateMachineHeaders.JOIN_REQUEST, joinRequest)
			.setHeader(WorkspaceJoinStateMachineHeaders.ACTOR, actor)
			.setHeader(WorkspaceJoinStateMachineHeaders.FAILURE_CODE, failureCode)
			.setHeader(WorkspaceJoinStateMachineHeaders.FAILURE_MESSAGE, failureMessage)
			.build();

		send(joinRequest, message);
	}

	private void send(
		WorkspaceJoinRequest joinRequest,
		Message<WorkspaceJoinRequestEvent> message
	) {
		StateMachine<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> stateMachine =
			stateMachineFactory.getStateMachine(machineId(joinRequest));

		try {
			stateMachine.stopReactively().block();
			reset(stateMachine, joinRequest.getStatus());
			stateMachine.getExtendedState().getVariables().remove(WorkspaceJoinStateMachineHeaders.EXCEPTION);
			stateMachine.startReactively().block();

			List<StateMachineEventResult<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent>> results = stateMachine
				.sendEventCollect(Mono.just(message))
				.block();

			if (results == null || results.stream().noneMatch(result -> result.getResultType() == ResultType.ACCEPTED)) {
				throwStoredException(stateMachine);
				throw new WorkspaceJoinRequestInvalidTransitionException();
			}

			results.forEach(result -> result.complete().block());
			throwStoredException(stateMachine);
		} finally {
			stateMachine.stopReactively().block();
		}
	}

	private void throwStoredException(
		StateMachine<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> stateMachine
	) {
		Object exception = stateMachine.getExtendedState().getVariables()
			.get(WorkspaceJoinStateMachineHeaders.EXCEPTION);
		if (exception instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
	}

	private void reset(
		StateMachine<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> stateMachine,
		WorkspaceJoinRequestStatus status
	) {
		StateMachineContext<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> context =
			new DefaultStateMachineContext<>(status, null, null, null);

		stateMachine.getStateMachineAccessor().doWithAllRegions(access ->
			access.resetStateMachineReactively(context).block()
		);
	}

	private String machineId(WorkspaceJoinRequest joinRequest) {
		return STATE_MACHINE_ID.formatted(joinRequest.getId());
	}
}
