package dev.runtime_lab.flowit.domain.workspace.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkspaceMemberRoleTest {

	@Test
	void authorityUsesWorkspaceScope() {
		assertEquals("WORKSPACE_OWNER", WorkspaceMemberRole.OWNER.authority());
		assertEquals("WORKSPACE_ADMIN", WorkspaceMemberRole.ADMIN.authority());
		assertEquals("WORKSPACE_MEMBER", WorkspaceMemberRole.MEMBER.authority());
	}
}
