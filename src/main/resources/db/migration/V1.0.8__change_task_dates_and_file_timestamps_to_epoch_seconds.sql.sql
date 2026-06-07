ALTER TABLE tasks
    ADD COLUMN start_date_epoch BIGINT UNSIGNED NULL
        COMMENT '작업 시작 예정 일자 입니다. (epoch,UTC midnight.)'
        AFTER assignee_workspace_member_id,
    ADD COLUMN due_date_epoch BIGINT UNSIGNED NULL
        COMMENT '작업 종료 예정 일자 입니다. (epoch,UTC midnight.)'
        AFTER start_date_epoch;

UPDATE tasks
SET start_date_epoch = CASE
        WHEN start_date IS NULL THEN NULL
        ELSE TIMESTAMPDIFF(SECOND, '1970-01-01 00:00:00', start_date)
    END,
    due_date_epoch = CASE
        WHEN due_date IS NULL THEN NULL
        ELSE TIMESTAMPDIFF(SECOND, '1970-01-01 00:00:00', due_date)
    END;

ALTER TABLE tasks
    DROP INDEX idx_tasks_workspace_due_date,
    DROP COLUMN start_date,
    DROP COLUMN due_date,
    CHANGE COLUMN start_date_epoch start_date BIGINT UNSIGNED NULL
        COMMENT '작업 시작 예정 일자 입니다. (epoch,UTC midnight.)',
    CHANGE COLUMN due_date_epoch due_date BIGINT UNSIGNED NULL
        COMMENT '작업 마감 예정 일자 입니다. (epoch,UTC midnight.)',
    ADD KEY idx_tasks_workspace_due_date (workspace_id, due_date);

UPDATE files
SET created_at = CASE
                     WHEN created_at >= 100000000000 THEN created_at DIV 1000
                     ELSE created_at
    END,
    updated_at = CASE
                     WHEN updated_at >= 100000000000 THEN updated_at DIV 1000
                     ELSE updated_at
        END,
    deleted_at = CASE
                     WHEN deleted_at IS NULL THEN NULL
                     WHEN deleted_at >= 100000000000 THEN deleted_at DIV 1000
                     ELSE deleted_at
        END;

ALTER TABLE files
    MODIFY COLUMN created_at BIGINT UNSIGNED NOT NULL
    COMMENT '파일 메타데이터 생성 일시. Unix epoch seconds 기준.',
    MODIFY COLUMN updated_at BIGINT UNSIGNED NOT NULL
    COMMENT '파일 메타데이터 최종 수정 일시. Unix epoch seconds 기준.',
    MODIFY COLUMN deleted_at BIGINT UNSIGNED NULL
    COMMENT '파일 소프트 삭제 일시. Unix epoch seconds 기준.';

