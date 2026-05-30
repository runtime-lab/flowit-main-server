package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
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

		Files.createDirectories(userStatusSnippetPath.getParent());
		Files.writeString(
			userStatusSnippetPath,
			enumTable(UserStatus.values(), Map.of(
				UserStatus.ACTIVE, "정상적으로 사용할 수 있는 활성 사용자입니다.",
				UserStatus.LOCKED, "잠금 처리되어 인증 또는 주요 기능 사용이 제한된 사용자입니다.",
				UserStatus.WITHDRAWN, "탈퇴 처리된 사용자입니다."
			)),
			StandardCharsets.UTF_8
		);
		Files.writeString(
			workspaceMemberRoleSnippetPath,
			enumTable(WorkspaceMemberRole.values(), Map.of(
				WorkspaceMemberRole.OWNER, "워크스페이스 소유자 권한입니다.",
				WorkspaceMemberRole.ADMIN, "워크스페이스를 관리할 수 있는 관리자 권한입니다.",
				WorkspaceMemberRole.MEMBER, "워크스페이스에 참여한 일반 멤버 권한입니다."
			)),
			StandardCharsets.UTF_8
		);

		assertTrue(Files.exists(userStatusSnippetPath));
		assertTrue(Files.exists(workspaceMemberRoleSnippetPath));
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
			builder.append("|`").append(value.name()).append("`\n");
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
