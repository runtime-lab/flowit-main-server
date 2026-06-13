package dev.runtime_lab.flowit.domain.task.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class TaskCommentNotFoundException extends TaskException {

	public TaskCommentNotFoundException() {
		super(ErrorCode.TASK_COMMENT_404_001, "존재하지 않는 작업 댓글입니다.");
	}
}
