package dev.runtime_lab.flowit.docs;

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

class EnumReferenceDocsTest {

	@Test
	void generateEnumReferenceTablesFromEnums() throws Exception {
		Path userStatusSnippetPath = Path.of("build/generated-snippets/enum-reference/user-status.adoc");
		Path workspaceMemberRoleSnippetPath = Path.of("build/generated-snippets/enum-reference/workspace-member-role.adoc");
		Path workspaceJoinRequestStatusSnippetPath =
			Path.of("build/generated-snippets/enum-reference/workspace-join-request-status.adoc");
		Path workspaceJoinRequestMethodSnippetPath =
			Path.of("build/generated-snippets/enum-reference/workspace-join-request-method.adoc");

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

		assertTrue(Files.exists(userStatusSnippetPath));
		assertTrue(Files.exists(workspaceMemberRoleSnippetPath));
		assertTrue(Files.exists(workspaceJoinRequestStatusSnippetPath));
		assertTrue(Files.exists(workspaceJoinRequestMethodSnippetPath));
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
