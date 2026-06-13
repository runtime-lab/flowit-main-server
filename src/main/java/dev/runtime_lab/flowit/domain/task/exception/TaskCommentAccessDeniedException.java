package dev.runtime_lab.flowit.domain.task.exception;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;

public class TaskCommentAccessDeniedException extends TaskException {

	public TaskCommentAccessDeniedException() {
		super(ErrorCode.AUTH_403_001, "작업 댓글은 작성자만 수정하거나 삭제할 수 있습니다.");
	}
}
