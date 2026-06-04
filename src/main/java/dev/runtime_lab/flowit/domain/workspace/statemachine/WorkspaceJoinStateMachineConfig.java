package dev.runtime_lab.flowit.domain.workspace.statemachine;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
@EnableStateMachineFactory
@RequiredArgsConstructor
public class WorkspaceJoinStateMachineConfig
	extends StateMachineConfigurerAdapter<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> {

	private final WorkspaceJoinReadyGuard readyGuard;
	private final WorkspaceJoinReadyAction readyAction;
	private final WorkspaceJoinApproveAction approveAction;
	private final WorkspaceJoinAction joinAction;
	private final WorkspaceJoinFailAction failAction;

	@Override
	public void configure(StateMachineStateConfigurer<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> states)
		throws Exception {
		states
			.withStates()
			.initial(WorkspaceJoinRequestStatus.PENDING)
			.states(EnumSet.allOf(WorkspaceJoinRequestStatus.class));
	}

	@Override
	public void configure(
		StateMachineTransitionConfigurer<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> transitions
	) throws Exception {
		transitions
			.withExternal()
				.source(WorkspaceJoinRequestStatus.PENDING)
				.target(WorkspaceJoinRequestStatus.READY)
				.event(WorkspaceJoinRequestEvent.MARK_READY)
				.guard(readyGuard)
				.action(readyAction)
			.and()
			.withExternal()
				.source(WorkspaceJoinRequestStatus.READY)
				.target(WorkspaceJoinRequestStatus.APPROVED)
				.event(WorkspaceJoinRequestEvent.APPROVE)
				.action(approveAction)
			.and()
			.withExternal()
				.source(WorkspaceJoinRequestStatus.APPROVED)
				.target(WorkspaceJoinRequestStatus.JOINED)
				.event(WorkspaceJoinRequestEvent.JOIN)
				.action(joinAction)
			.and()
			.withExternal()
				.source(WorkspaceJoinRequestStatus.PENDING)
				.target(WorkspaceJoinRequestStatus.FAILED)
				.event(WorkspaceJoinRequestEvent.FAIL)
				.action(failAction)
			.and()
			.withExternal()
				.source(WorkspaceJoinRequestStatus.READY)
				.target(WorkspaceJoinRequestStatus.FAILED)
				.event(WorkspaceJoinRequestEvent.FAIL)
				.action(failAction)
			.and()
			.withExternal()
				.source(WorkspaceJoinRequestStatus.APPROVED)
				.target(WorkspaceJoinRequestStatus.FAILED)
				.event(WorkspaceJoinRequestEvent.FAIL)
				.action(failAction);
	}
}
