package dev.runtime_lab.flowit.domain.task.dto;

import dev.runtime_lab.flowit.domain.task.entity.TaskComment;

public record TaskCommentCreateResponse(
	Long id,
	Long createdAt
) {

	public static TaskCommentCreateResponse from(TaskComment comment) {
		return new TaskCommentCreateResponse(comment.getId(), comment.getCreatedAt());
	}
}
