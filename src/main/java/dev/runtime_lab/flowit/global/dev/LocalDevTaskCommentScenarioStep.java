package dev.runtime_lab.flowit.global.dev;

import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentUpdateRequest;
import dev.runtime_lab.flowit.domain.task.entity.TaskComment;
import dev.runtime_lab.flowit.domain.task.repository.TaskCommentRepository;
import dev.runtime_lab.flowit.domain.task.service.TaskCommentService;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDevTaskCommentScenarioStep {

	private final TaskCommentService taskCommentService;
	private final TaskCommentRepository taskCommentRepository;
	private final LocalDevSampleRecordRegistry sampleRecordRegistry;

	public void reconcile(LocalDevScenarioContext context) {
		for (LocalDevScenarioSamples.SampleTaskComment sampleComment : LocalDevScenarioSamples.TASK_COMMENTS) {
			Long taskId = context.taskId(sampleComment.taskRecordKey())
				.orElseThrow(() -> new IllegalStateException("Local development sample task was not created."));
			Long commentId = reconcileComment(context, taskId, sampleComment);
			sampleRecordRegistry.upsert(
				sampleComment.recordKey(),
				LocalDevSampleRecordType.TASK_COMMENT,
				commentId
			);
		}
	}

	private Long reconcileComment(
		LocalDevScenarioContext context,
		Long taskId,
		LocalDevScenarioSamples.SampleTaskComment sampleComment
	) {
		String contentMarkdown = sampleComment.contentMarkdown().trim();
		return findTrackedComment(context, taskId, sampleComment)
			.map(comment -> reconcileExistingComment(context, taskId, comment, contentMarkdown))
			.or(() -> taskCommentRepository.findActiveByTaskIdAndContentMarkdown(taskId, contentMarkdown)
				.map(TaskComment::getId))
			.orElseGet(() -> createComment(context, taskId, contentMarkdown));
	}

	private Optional<TaskComment> findTrackedComment(
		LocalDevScenarioContext context,
		Long taskId,
		LocalDevScenarioSamples.SampleTaskComment sampleComment
	) {
		return sampleRecordRegistry.findEntityId(sampleComment.recordKey(), LocalDevSampleRecordType.TASK_COMMENT)
			.flatMap(commentId -> taskCommentRepository.findActiveByWorkspaceIdAndTaskIdAndCommentIdForUpdate(
				context.workspace().getId(),
				taskId,
				commentId
			));
	}

	private Long reconcileExistingComment(
		LocalDevScenarioContext context,
		Long taskId,
		TaskComment comment,
		String contentMarkdown
	) {
		if (!Objects.equals(comment.getContentMarkdown(), contentMarkdown)) {
			taskCommentService.update(
				context.currentUser(),
				context.workspace().getId(),
				taskId,
				comment.getId(),
				new TaskCommentUpdateRequest(contentMarkdown)
			);
		}

		return comment.getId();
	}

	private Long createComment(
		LocalDevScenarioContext context,
		Long taskId,
		String contentMarkdown
	) {
		TaskCommentCreateResponse response = taskCommentService.create(
			context.currentUser(),
			context.workspace().getId(),
			taskId,
			new TaskCommentCreateRequest(contentMarkdown)
		);
		return response.id();
	}
}
