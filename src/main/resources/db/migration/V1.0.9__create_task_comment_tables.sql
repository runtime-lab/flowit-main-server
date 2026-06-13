CREATE TABLE task_comments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Task comment id.',
    workspace_id BIGINT UNSIGNED NOT NULL COMMENT 'Workspace id where the task comment belongs.',
    task_id BIGINT UNSIGNED NOT NULL COMMENT 'Task id where the comment belongs.',
    author_workspace_member_id BIGINT UNSIGNED NOT NULL COMMENT 'Workspace member id of the comment author.',
    author_user_id BIGINT UNSIGNED NOT NULL COMMENT 'User id of the comment author.',
    author_display_name_snapshot VARCHAR(100) NOT NULL COMMENT 'Author display name snapshot at comment creation time.',
    content_markdown TEXT NOT NULL COMMENT 'Task comment markdown content.',
    edited TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether the comment content has been edited after creation.',
    created_at BIGINT UNSIGNED NOT NULL COMMENT 'Comment creation time as Unix epoch seconds.',
    updated_at BIGINT UNSIGNED NOT NULL COMMENT 'Comment last update time as Unix epoch seconds.',
    deleted_at BIGINT UNSIGNED NULL COMMENT 'Comment soft deletion time as Unix epoch seconds.',

    PRIMARY KEY (id),
    KEY idx_task_comments_task_created (task_id, created_at, id),
    KEY idx_task_comments_workspace_created (workspace_id, created_at, id),
    KEY idx_task_comments_author_member (author_workspace_member_id),
    KEY idx_task_comments_author_user (author_user_id),
    KEY idx_task_comments_deleted_at (deleted_at),

    CONSTRAINT fk_task_comments_workspace
        FOREIGN KEY (workspace_id)
            REFERENCES workspaces (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_task_comments_task
        FOREIGN KEY (task_id)
            REFERENCES tasks (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_task_comments_author_member
        FOREIGN KEY (author_workspace_member_id)
            REFERENCES workspace_members (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_task_comments_author_user
        FOREIGN KEY (author_user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Task comments attached to workspace tasks.';
