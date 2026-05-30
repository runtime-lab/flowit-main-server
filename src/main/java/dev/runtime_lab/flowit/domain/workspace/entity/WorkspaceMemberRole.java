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
}
