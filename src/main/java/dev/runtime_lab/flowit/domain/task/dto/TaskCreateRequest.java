package dev.runtime_lab.flowit.domain.task.dto;

import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.DESCRIPTION_MARKDOWN_MAX_LENGTH;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.PROGRESS_MAX;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.PROGRESS_MIN;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.TAG_MAX_COUNT;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.TAG_MAX_LENGTH;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.TITLE_MAX_LENGTH;

public record TaskCreateRequest(
	@NotBlank
	@Size(max = TITLE_MAX_LENGTH)
	String title,

	@Size(max = DESCRIPTION_MARKDOWN_MAX_LENGTH)
	String descriptionMarkdown,

	TaskStatus status,

	Long assigneeMemberId,

	@NotNull
	TaskPriority priority,

	Long startDate,

	Long dueDate,

	@Min(PROGRESS_MIN)
	@Max(PROGRESS_MAX)
	Integer progress,

	@Size(max = TAG_MAX_COUNT)
	List<@NotBlank @Size(max = TAG_MAX_LENGTH) String> tags
) {
}
