package dev.runtime_lab.flowit.domain.workspace.statemachine;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestEvent;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestHistory;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceJoinRequestHistoryRepository;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceMemberRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceJoinAction extends WorkspaceJoinStateMachineSupport
	implements Action<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> {

	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceJoinRequestHistoryRepository historyRepository;
	private final Clock clock;

	@Override
	public void execute(StateContext<WorkspaceJoinRequestStatus, WorkspaceJoinRequestEvent> context) {
		WorkspaceJoinRequest joinRequest = joinRequest(context);
		Long now = now();

		WorkspaceMember workspaceMember = workspaceMemberRepository.save(WorkspaceMember.builder()
			.workspace(joinRequest.getWorkspace())
			.user(joinRequest.getUser())
			.role(WorkspaceMemberRole.MEMBER)
			.joinedAt(now)
			.createdAt(now)
			.updatedAt(now)
			.build());

		WorkspaceJoinRequestHistory history = joinRequest.join(workspaceMember, actor(context), now);
		historyRepository.save(history);
	}

	private Long now() {
		return Instant.now(clock).getEpochSecond();
	}
}
