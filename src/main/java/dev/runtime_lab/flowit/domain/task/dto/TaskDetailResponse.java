package dev.runtime_lab.flowit.domain.task.dto;

import dev.runtime_lab.flowit.domain.task.entity.Task;
import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import dev.runtime_lab.flowit.global.web.response.ApiListData;
import java.util.List;

public record TaskDetailResponse(
	Long id,
	Long workspaceId,
	String title,
	String descriptionMarkdown,
	TaskStatus status,
	TaskAssigneeResponse assignee,
	TaskPriority priority,
	Long startDate,
	Long dueDate,
	List<String> tags,
	Integer progress,
	Long createdByUserId,
	Long createdAt,
	Long updatedAt,
	ApiListData<TaskCommentResponse> commentPage
) {

	public static TaskDetailResponse from(Task task, List<String> tags) {
		return from(task, tags, ApiListData.of(List.of(), 0L));
	}

	public static TaskDetailResponse from(
		Task task,
		List<String> tags,
		ApiListData<TaskCommentResponse> commentPage
	) {
		return new TaskDetailResponse(
			task.getId(),
			task.getWorkspace().getId(),
			task.getTitle(),
			task.getDescriptionMarkdown(),
			task.getStatus(),
			TaskAssigneeResponse.from(task.getAssignee()),
			task.getPriority(),
			task.getStartDate(),
			task.getDueDate(),
			List.copyOf(tags),
			task.getProgress(),
			task.getCreatedBy().getId(),
			task.getCreatedAt(),
			task.getUpdatedAt(),
			commentPage
		);
	}
}
