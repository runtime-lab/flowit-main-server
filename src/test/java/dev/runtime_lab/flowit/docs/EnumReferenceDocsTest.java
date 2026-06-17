package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.activity.dto.ActivityChangeElement;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordAction;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordDomain;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordTopic;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityTargetType;
import dev.runtime_lab.flowit.domain.task.entity.TaskHistoryAction;
import dev.runtime_lab.flowit.domain.task.entity.TaskHistoryElement;
import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestMethod;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceJoinRequestStatus;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMemberRole;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static java.util.Map.entry;

class EnumReferenceDocsTest {

	@Test
	void generateEnumReferenceTablesFromEnums() throws Exception {
		Path userStatusSnippetPath = Path.of("build/generated-snippets/enum-reference/user-status.adoc");
		Path workspaceMemberRoleSnippetPath = Path.of("build/generated-snippets/enum-reference/workspace-member-role.adoc");
		Path workspaceJoinRequestStatusSnippetPath =
			Path.of("build/generated-snippets/enum-reference/workspace-join-request-status.adoc");
		Path workspaceJoinRequestMethodSnippetPath =
			Path.of("build/generated-snippets/enum-reference/workspace-join-request-method.adoc");
		Path taskStatusSnippetPath = Path.of("build/generated-snippets/enum-reference/task-status.adoc");
		Path taskPrioritySnippetPath = Path.of("build/generated-snippets/enum-reference/task-priority.adoc");
		Path taskHistoryActionSnippetPath = Path.of("build/generated-snippets/enum-reference/task-history-action.adoc");
		Path taskHistoryElementSnippetPath = Path.of("build/generated-snippets/enum-reference/task-history-element.adoc");
		Path activityRecordTopicSnippetPath = Path.of("build/generated-snippets/enum-reference/activity-record-topic.adoc");
		Path activityRecordDomainSnippetPath = Path.of("build/generated-snippets/enum-reference/activity-record-domain.adoc");
		Path activityRecordActionSnippetPath = Path.of("build/generated-snippets/enum-reference/activity-record-action.adoc");
		Path activityTargetTypeSnippetPath = Path.of("build/generated-snippets/enum-reference/activity-target-type.adoc");
		Path activityChangeElementSnippetPath = Path.of("build/generated-snippets/enum-reference/activity-change-element.adoc");

		Files.createDirectories(userStatusSnippetPath.getParent());
		Files.writeString(
			userStatusSnippetPath,
			enumTable(UserStatus.values(), Map.of(
				UserStatus.ACTIVE, "활성 사용자 계정입니다.",
				UserStatus.LOCKED, "잠긴 사용자 계정입니다.",
				UserStatus.WITHDRAWN, "탈퇴한 사용자 계정입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			workspaceMemberRoleSnippetPath,
			enumTable(WorkspaceMemberRole.values(), Map.of(
				WorkspaceMemberRole.OWNER, "워크스페이스 소유자 역할입니다.",
				WorkspaceMemberRole.ADMIN, "워크스페이스 관리자 역할입니다.",
				WorkspaceMemberRole.MEMBER, "워크스페이스 일반 멤버 역할입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			workspaceJoinRequestStatusSnippetPath,
			enumTable(WorkspaceJoinRequestStatus.values(), Map.of(
				WorkspaceJoinRequestStatus.PENDING, "워크스페이스 가입 요청이 생성된 상태입니다.",
				WorkspaceJoinRequestStatus.READY, "가입 요청이 기본 검증을 통과해 승인 가능한 상태입니다.",
				WorkspaceJoinRequestStatus.APPROVED, "가입 요청이 승인된 상태입니다.",
				WorkspaceJoinRequestStatus.JOINED, "워크스페이스 멤버십 생성이 완료된 상태입니다.",
				WorkspaceJoinRequestStatus.FAILED, "요청 생성 이후 가입 흐름 처리에 실패한 상태입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			workspaceJoinRequestMethodSnippetPath,
			enumTable(WorkspaceJoinRequestMethod.values(), Map.of(
				WorkspaceJoinRequestMethod.INVITE_CODE, "초대 코드로 생성된 워크스페이스 가입 요청입니다."
			)),
			StandardCharsets.UTF_8
		);

		Files.writeString(
			taskStatusSnippetPath,
			enumTable(TaskStatus.values(), Map.of(
				TaskStatus.TODO, "아직 시작하지 않은 작업 상태입니다.",
				TaskStatus.IN_PROGRESS, "진행 중인 작업 상태입니다.",
				TaskStatus.DONE, "완료된 작업 상태입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			taskPrioritySnippetPath,
			enumTable(TaskPriority.values(), Map.of(
				TaskPriority.HIGH, "높은 우선순위 작업입니다.",
				TaskPriority.MEDIUM, "보통 우선순위 작업입니다.",
				TaskPriority.LOW, "낮은 우선순위 작업입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			taskHistoryActionSnippetPath,
			enumTable(TaskHistoryAction.values(), Map.of(
				TaskHistoryAction.CREATED, "작업이 생성된 변경 이력입니다.",
				TaskHistoryAction.MODIFIED, "작업 일반 필드가 수정된 변경 이력입니다.",
				TaskHistoryAction.STATUS_CHANGED, "작업 상태가 변경된 이력입니다.",
				TaskHistoryAction.PROGRESS_CHANGED, "작업 진행도가 변경된 이력입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			taskHistoryElementSnippetPath,
			enumTable(TaskHistoryElement.values(), Map.of(
				TaskHistoryElement.TITLE, "작업 제목 변경 요소입니다.",
				TaskHistoryElement.DESCRIPTION, "작업 설명 변경 요소입니다.",
				TaskHistoryElement.STATUS, "작업 상태 변경 요소입니다.",
				TaskHistoryElement.ASSIGNEE, "작업 담당자 변경 요소입니다.",
				TaskHistoryElement.PRIORITY, "작업 우선순위 변경 요소입니다.",
				TaskHistoryElement.START_DATE, "작업 시작 예정일 변경 요소입니다.",
				TaskHistoryElement.DUE_DATE, "작업 마감 예정일 변경 요소입니다.",
				TaskHistoryElement.TAGS, "작업 태그 목록 변경 요소입니다.",
				TaskHistoryElement.PROGRESS, "작업 진행도 변경 요소입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			activityRecordTopicSnippetPath,
			enumTable(ActivityRecordTopic.values(), Map.of(
				ActivityRecordTopic.ALL, "작업과 멤버 활동을 모두 조회합니다.",
				ActivityRecordTopic.TASK, "작업 활동만 조회합니다.",
				ActivityRecordTopic.MEMBER, "워크스페이스 멤버 활동만 조회합니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			activityRecordDomainSnippetPath,
			enumTable(ActivityRecordDomain.values(), Map.of(
				ActivityRecordDomain.TASK, "작업 도메인에서 발생한 활동입니다.",
				ActivityRecordDomain.WORKSPACE_MEMBER, "워크스페이스 멤버십 도메인에서 발생한 활동입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			activityRecordActionSnippetPath,
			enumTable(ActivityRecordAction.values(), Map.of(
				ActivityRecordAction.CREATED, "작업 생성 활동입니다.",
				ActivityRecordAction.MODIFIED, "작업 일반 수정 활동입니다.",
				ActivityRecordAction.STATUS_CHANGED, "작업 상태 변경 활동입니다.",
				ActivityRecordAction.PROGRESS_CHANGED, "작업 진행도 변경 활동입니다.",
				ActivityRecordAction.ROLE_CHANGED, "워크스페이스 멤버 역할 변경 활동입니다.",
				ActivityRecordAction.REMOVED, "워크스페이스 멤버 강제 퇴장 활동입니다.",
				ActivityRecordAction.WITHDRAWN, "워크스페이스 멤버 자진 탈퇴 활동입니다.",
				ActivityRecordAction.JOINED, "워크스페이스 멤버 가입 활동입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			activityTargetTypeSnippetPath,
			enumTable(ActivityTargetType.values(), Map.of(
				ActivityTargetType.TASK, "활동 대상이 작업입니다.",
				ActivityTargetType.WORKSPACE_MEMBER, "활동 대상이 워크스페이스 멤버입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			activityChangeElementSnippetPath,
			enumTable(ActivityChangeElement.values(), Map.ofEntries(
				entry(ActivityChangeElement.TITLE, "작업 제목 변경 요소입니다."),
				entry(ActivityChangeElement.DESCRIPTION, "작업 설명 변경 요소입니다."),
				entry(ActivityChangeElement.STATUS, "작업 상태 변경 요소입니다."),
				entry(ActivityChangeElement.ASSIGNEE, "작업 담당자 변경 요소입니다."),
				entry(ActivityChangeElement.PRIORITY, "작업 우선순위 변경 요소입니다."),
				entry(ActivityChangeElement.START_DATE, "작업 시작 예정일 변경 요소입니다."),
				entry(ActivityChangeElement.DUE_DATE, "작업 마감 예정일 변경 요소입니다."),
				entry(ActivityChangeElement.TAGS, "작업 태그 목록 변경 요소입니다."),
				entry(ActivityChangeElement.PROGRESS, "작업 진행도 변경 요소입니다."),
				entry(ActivityChangeElement.ROLE, "워크스페이스 멤버 역할 변경 요소입니다."),
				entry(ActivityChangeElement.MEMBERSHIP, "워크스페이스 멤버십 생명주기 변경 요소입니다."),
				entry(ActivityChangeElement.OWNERSHIP_TRANSFER, "워크스페이스 소유권 이전 변경 요소입니다.")
			)),
			StandardCharsets.UTF_8
		);

		assertTrue(Files.exists(userStatusSnippetPath));
		assertTrue(Files.exists(workspaceMemberRoleSnippetPath));
		assertTrue(Files.exists(workspaceJoinRequestStatusSnippetPath));
		assertTrue(Files.exists(workspaceJoinRequestMethodSnippetPath));
		assertTrue(Files.exists(taskStatusSnippetPath));
		assertTrue(Files.exists(taskPrioritySnippetPath));
		assertTrue(Files.exists(taskHistoryActionSnippetPath));
		assertTrue(Files.exists(taskHistoryElementSnippetPath));
		assertTrue(Files.exists(activityRecordTopicSnippetPath));
		assertTrue(Files.exists(activityRecordDomainSnippetPath));
		assertTrue(Files.exists(activityRecordActionSnippetPath));
		assertTrue(Files.exists(activityTargetTypeSnippetPath));
		assertTrue(Files.exists(activityChangeElementSnippetPath));
	}

	private <E extends Enum<E>> String enumTable(E[] values, Map<E, String> descriptions) {
		assertEquals(values.length, descriptions.size());
		assertTrue(descriptions.keySet().containsAll(Arrays.asList(values)));

		StringBuilder builder = new StringBuilder();
		builder.append("[cols=\"1,4\",options=\"header\",role=\"api-table\"]\n");
		builder.append("|===\n");
		builder.append("|값 |설명\n");

		for (E value : values) {
			builder.append('\n');
			builder.append("|``").append(value.name()).append("``\n");
			builder.append("|").append(escapeCell(descriptions.get(value))).append('\n');
		}

		builder.append("|===\n");
		return builder.toString();
	}

	private String escapeCell(String value) {
		return value
			.replace("|", "\\|")
			.replace("\r", " ")
			.replace("\n", " ");
	}
}
