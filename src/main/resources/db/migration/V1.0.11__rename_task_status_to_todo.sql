UPDATE tasks
SET status = 'TODO'
WHERE status = 'TO_DO';

UPDATE task_change_histories
SET changes_json = REPLACE(changes_json, '"TO_DO"', '"TODO"')
WHERE changes_json LIKE '%"TO_DO"%';

UPDATE workspace_activity_records
SET changes_json = REPLACE(changes_json, '"TO_DO"', '"TODO"')
WHERE changes_json LIKE '%"TO_DO"%';

ALTER TABLE tasks
    MODIFY COLUMN status VARCHAR(30) NOT NULL
        COMMENT '작업 상태입니다. TODO, IN_PROGRESS, DONE 중 하나입니다.';
