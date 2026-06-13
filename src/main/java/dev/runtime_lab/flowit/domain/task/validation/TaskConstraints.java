package dev.runtime_lab.flowit.domain.task.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskConstraints {

	public static final int TITLE_MAX_LENGTH = 100;
	public static final int DESCRIPTION_MARKDOWN_MAX_LENGTH = 10000;
	public static final int COMMENT_MARKDOWN_MAX_LENGTH = 1000;
	public static final int TAG_MAX_COUNT = 10;
	public static final int TAG_MAX_LENGTH = 30;
	public static final int PROGRESS_MIN = 0;
	public static final int PROGRESS_MAX = 100;
	public static final int MIN_PAGE = 0;
	public static final int MIN_PAGE_SIZE = 1;
	public static final int MAX_SEARCH_PAGE_SIZE = 200;
	public static final int DEFAULT_COMMENT_PAGE_SIZE = 20;
	public static final int MAX_COMMENT_PAGE_SIZE = 200;
	public static final int DEFAULT_HISTORY_PAGE_SIZE = 50;
	public static final int MAX_HISTORY_PAGE_SIZE = 200;
}
