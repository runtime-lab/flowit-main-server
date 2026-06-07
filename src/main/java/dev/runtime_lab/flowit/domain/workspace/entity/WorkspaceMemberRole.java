package dev.runtime_lab.flowit.domain.workspace.entity;

public enum WorkspaceMemberRole {
	OWNER("WORKSPACE_OWNER"),
	ADMIN("WORKSPACE_ADMIN"),
	MEMBER("WORKSPACE_MEMBER");

	private final String authority;

	WorkspaceMemberRole(String authority) {
		this.authority = authority;
	}

	public String authority() {
		return authority;
	}

	public boolean isWorkspaceOwner() {
		return this == OWNER;
	}

	public boolean isWorkspaceAdmin() {
		return this == ADMIN;
	}

	public boolean isWorkspaceMember() {
		return this == MEMBER;
	}

	public boolean canRemoveMember() {
		return this == OWNER || this == ADMIN;
	}

	public boolean canManageJoinRequests() {
		return this == OWNER || this == ADMIN;
	}

	public boolean canUpdateMemberRoleTo(WorkspaceMemberRole targetRole) {
		if (this == OWNER) {
			return true;
		}

		if (this == ADMIN) {
			return targetRole != OWNER;
		}

		return false;
	}

	public boolean canUpdateWorkspace() {
		return this == OWNER || this == ADMIN;
	}
}
