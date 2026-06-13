package dev.runtime_lab.flowit.domain.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.COMMENT_MARKDOWN_MAX_LENGTH;

public record TaskCommentUpdateRequest(
	@NotBlank
	@Size(max = COMMENT_MARKDOWN_MAX_LENGTH)
	String contentMarkdown
) {
}
