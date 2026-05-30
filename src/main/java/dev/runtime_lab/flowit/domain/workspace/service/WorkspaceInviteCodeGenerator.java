package dev.runtime_lab.flowit.domain.workspace.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceInviteCodeGenerator {

	private static final char[] INVITE_CODE_SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	private static final int GROUP_LENGTH = 4;
	private static final int GROUP_COUNT = 3;

	private final SecureRandom secureRandom = new SecureRandom();

	public String generate() {
		StringBuilder builder = new StringBuilder(14);
		for (int group = 0; group < GROUP_COUNT; group++) {
			if (group > 0) {
				builder.append('-');
			}
			appendGroup(builder);
		}

		return builder.toString();
	}

	private void appendGroup(StringBuilder builder) {
		for (int index = 0; index < GROUP_LENGTH; index++) {
			builder.append(INVITE_CODE_SYMBOLS[secureRandom.nextInt(INVITE_CODE_SYMBOLS.length)]);
		}
	}
}
