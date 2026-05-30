package dev.runtime_lab.flowit.domain.workspace.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceInviteCodeGeneratorTest {

	private final WorkspaceInviteCodeGenerator generator = new WorkspaceInviteCodeGenerator();

	@Test
	void generateReturnsFormattedInviteCode() {
		String inviteCode = generator.generate();

		assertTrue(inviteCode.matches("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}"));
	}
}
