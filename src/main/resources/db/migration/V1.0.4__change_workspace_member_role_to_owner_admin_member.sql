ALTER TABLE workspace_members
    DROP INDEX uk_workspace_members_active_leader_workspace;

ALTER TABLE workspace_members
    DROP COLUMN active_leader_workspace_id;

UPDATE workspace_members
SET role = 'OWNER'
WHERE role = 'LEADER';

ALTER TABLE workspace_members
    ADD COLUMN active_owner_workspace_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted_at IS NULL AND role = 'OWNER' THEN workspace_id
                ELSE NULL
            END
        ) STORED COMMENT 'Ensures one active owner per workspace.';

ALTER TABLE workspace_members
    ADD UNIQUE KEY uk_workspace_members_active_owner_workspace (active_owner_workspace_id);
