package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.global.web.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorCodeDocsTest {

	@Test
	void generateErrorCodeTableFromEnum() throws Exception {
		Path snippetPath = Path.of("build/generated-snippets/error-codes/error-codes.adoc");

		Files.createDirectories(snippetPath.getParent());
		Files.writeString(snippetPath, errorCodeTable(), StandardCharsets.UTF_8);

		assertTrue(Files.exists(snippetPath));
	}

	private String errorCodeTable() {
		StringBuilder builder = new StringBuilder();
		builder.append("[cols=\"24,18,58\",options=\"header\",role=\"error-code-table\"]\n");
		builder.append("|===\n");
		builder.append("|코드 |HTTP 상태 |클라이언트 참고\n");

		for (ErrorCode errorCode : ErrorCode.values()) {
			builder.append('\n');
			builder.append("|`").append(errorCode.getCode()).append("`\n");
			builder.append("|`")
				.append(errorCode.getHttpStatus().value())
				.append(' ')
				.append(errorCode.getHttpStatus().getReasonPhrase())
				.append("`\n");
			builder.append("a|")
				.append("**기본 메시지** +\n")
				.append(escapeCell(errorCode.getMessage()))
				.append("\n\n")
				.append("**설명** +\n")
				.append(escapeCell(errorCode.getDescription()))
				.append('\n');
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
