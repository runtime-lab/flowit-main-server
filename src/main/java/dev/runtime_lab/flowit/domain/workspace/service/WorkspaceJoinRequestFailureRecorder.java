package dev.runtime_lab.flowit.domain.workspace.service;

import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequest;
import dev.runtime_lab.flowit.domain.workspace.exception.WorkspaceJoinRequestInvalidTransitionException;
import dev.runtime_lab.flowit.domain.workspace.repository.WorkspaceJoinRequestRepository;
import dev.runtime_lab.flowit.domain.workspace.statemachine.WorkspaceJoinStateMachineService;
import dev.runtime_lab.flowit.global.web.exception.FlowitException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceJoinRequestFailureRecorder {

	private static final int FAILURE_CODE_MAX_LENGTH = 100;
	private static final int FAILURE_MESSAGE_MAX_LENGTH = 500;

	private final WorkspaceJoinRequestRepository workspaceJoinRequestRepository;
	private final WorkspaceJoinStateMachineService workspaceJoinStateMachineService;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(Long joinRequestId, RuntimeException exception) {
		WorkspaceJoinRequest joinRequest = workspaceJoinRequestRepository.findByIdForUpdate(joinRequestId)
			.orElseThrow(WorkspaceJoinRequestInvalidTransitionException::new);

		workspaceJoinStateMachineService.sendFailure(
			joinRequest,
			joinRequest.getUser(),
			failureCode(exception),
			failureMessage(exception)
		);
	}

	private String failureCode(RuntimeException exception) {
		if (exception instanceof FlowitException flowitException) {
			return truncate(flowitException.getErrorCode().getCode(), FAILURE_CODE_MAX_LENGTH);
		}

		return truncate(exception.getClass().getSimpleName(), FAILURE_CODE_MAX_LENGTH);
	}

	private String failureMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			message = exception.getClass().getSimpleName();
		}

		return truncate(message, FAILURE_MESSAGE_MAX_LENGTH);
	}

	private String truncate(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}

		return value.substring(0, maxLength);
	}
}
