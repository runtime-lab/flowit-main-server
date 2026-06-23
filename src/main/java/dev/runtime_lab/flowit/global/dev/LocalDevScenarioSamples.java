package dev.runtime_lab.flowit.global.dev;

import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import java.util.List;

final class LocalDevScenarioSamples {

	static final String SCENARIO_KEY = "local-main-scenario";
	static final String WORKSPACE_RECORD_KEY = "workspace.sample";
	static final String LOCAL_OWNER_EMAIL = "local@flowit.dev";
	static final String LOCAL_OWNER_PASSWORD = "localpass123";
	static final String LOCAL_OWNER_NICKNAME = "시나리오 유저";
	static final String LOCAL_WORKSPACE_NAME = "Flowit 시나리오";
	static final String LOCAL_WORKSPACE_DESCRIPTION = "테스트 정보 공급용 워크스페이스 입니다.";

	static final List<SampleTask> TASKS = List.of(
		new SampleTask(
			"task.onboarding-checklist",
			"온보딩 체크리스트 검토",
			"""
			인증, 워크스페이스, 작업 화면의 기본 흐름을 확인합니다.

			- [ ] 로그인 후 내 정보 조회
			- [ ] 워크스페이스 목록 진입
			- [ ] 작업 상세 화면에서 **마크다운 렌더링** 확인
			""",
			TaskStatus.TODO,
			TaskPriority.HIGH,
			0,
			1L,
			3L,
			List.of("local", "backend")
		),
		new SampleTask(
			"task.local-client-integration",
			"로컬 클라이언트 연동",
			"""
			로컬 서버를 기준으로 클라이언트 API 연동 상태를 점검합니다.

			## 확인 범위

			1. 작업 목록 필터
			2. 담당자 표시
			3. 진행률 변경

			`PATCH /v1/workspaces/{workspaceId}/tasks/{taskId}/progress` 응답을 확인합니다.
			""",
			TaskStatus.IN_PROGRESS,
			TaskPriority.MEDIUM,
			45,
			0L,
			5L,
			List.of("local", "frontend")
		),
		new SampleTask(
			"task.release-note-draft",
			"릴리즈 노트 초안 준비",
			"""
			완료 상태 필터와 대시보드 집계를 확인하기 위한 샘플 작업입니다.

			> 이 작업은 완료 상태 UI와 활동 기록 표시를 검증하기 위한 기준 데이터입니다.

			관련 문서는 `/docs/index.html`에서 확인합니다.
			""",
			TaskStatus.DONE,
			TaskPriority.LOW,
			100,
			-3L,
			-1L,
			List.of("docs")
		)
	);

	static final List<SampleTaskComment> TASK_COMMENTS = List.of(
		new SampleTaskComment(
			"task-comment.onboarding-checklist.default",
			"task.onboarding-checklist",
			"댓글 생성, 목록 조회, 수정, 삭제 흐름을 확인하기 위한 샘플 댓글입니다."
		),
		new SampleTaskComment(
			"task-comment.local-client-integration.default",
			"task.local-client-integration",
			"클라이언트 연동 중 확인한 내용을 이 댓글 아래에 이어서 점검할 수 있습니다."
		)
	);

	private LocalDevScenarioSamples() {
	}

	record SampleTask(
		String recordKey,
		String title,
		String descriptionMarkdown,
		TaskStatus status,
		TaskPriority priority,
		Integer progress,
		Long startOffsetDays,
		Long dueOffsetDays,
		List<String> tags
	) {
	}

	record SampleTaskComment(
		String recordKey,
		String taskRecordKey,
		String contentMarkdown
	) {
	}
}
