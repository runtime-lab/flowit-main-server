package dev.runtime_lab.flowit.domain.task.dto;

import dev.runtime_lab.flowit.domain.task.entity.TaskComment;

public record TaskCommentAuthorResponse(
	Long memberId,
	String displayName
) {

	public static TaskCommentAuthorResponse from(TaskComment comment) {
		return new TaskCommentAuthorResponse(
			comment.getAuthorWorkspaceMember().getId(),
			comment.getAuthorDisplayNameSnapshot()
		);
	}
}
