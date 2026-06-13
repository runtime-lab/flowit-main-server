package dev.runtime_lab.flowit.domain.task.dto;

import dev.runtime_lab.flowit.domain.task.entity.TaskComment;
import java.util.Objects;

public record TaskCommentResponse(
	Long id,
	Long taskId,
	TaskCommentAuthorResponse author,
	String contentMarkdown,
	boolean edited,
	boolean editable,
	boolean ownedByRequester,
	Long createdAt,
	Long updatedAt
) {

	public static TaskCommentResponse from(TaskComment comment) {
		return from(comment, null);
	}

	public static TaskCommentResponse from(TaskComment comment, Long requesterUserId) {
		boolean ownedByRequester = Objects.equals(comment.getAuthorUser().getId(), requesterUserId);

		return new TaskCommentResponse(
			comment.getId(),
			comment.getTask().getId(),
			TaskCommentAuthorResponse.from(comment),
			comment.getContentMarkdown(),
			comment.isEdited(),
			ownedByRequester,
			ownedByRequester,
			comment.getCreatedAt(),
			comment.getUpdatedAt()
		);
	}
}
