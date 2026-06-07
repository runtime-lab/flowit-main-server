package dev.runtime_lab.flowit.domain.task.dto;

import dev.runtime_lab.flowit.domain.task.entity.Task;
import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import java.util.List;

public record TaskSummaryResponse(
	Long id,
	Long workspaceId,
	String title,
	TaskStatus status,
	TaskAssigneeResponse assignee,
	TaskPriority priority,
	Long startDate,
	Long dueDate,
	List<String> tags,
	Integer progress,
	Long createdAt,
	Long updatedAt
) {

	public static TaskSummaryResponse from(Task task, List<String> tags) {
		return new TaskSummaryResponse(
			task.getId(),
			task.getWorkspace().getId(),
			task.getTitle(),
			task.getStatus(),
			TaskAssigneeResponse.from(task.getAssignee()),
			task.getPriority(),
			task.getStartDate(),
			task.getDueDate(),
			List.copyOf(tags),
			task.getProgress(),
			task.getCreatedAt(),
			task.getUpdatedAt()
		);
	}
}
