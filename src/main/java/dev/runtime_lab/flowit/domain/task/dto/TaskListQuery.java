package dev.runtime_lab.flowit.domain.task.dto;

import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;

import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.MAX_SEARCH_PAGE_SIZE;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.MIN_PAGE_SIZE;

public record TaskListQuery(
	TaskStatus status,
	Long assigneeMemberId,
	String tag,
	String keyword,
	Long dueFrom,
	Long dueTo,
	Integer page,
	Integer size
) {

	public int pageOrDefault() {
		if (page == null || page < 0) {
			return 0;
		}

		return page;
	}

	public int sizeOrDefault() {
		if (size == null) {
			return 50;
		}
		if (size < 1) {
			return MIN_PAGE_SIZE;
		}
		if (size > MAX_SEARCH_PAGE_SIZE) {
			return MAX_SEARCH_PAGE_SIZE;
		}

		return size;
	}
}
