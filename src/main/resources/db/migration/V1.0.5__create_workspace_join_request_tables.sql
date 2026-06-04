ALTER TABLE users
    MODIFY COLUMN token_version BIGINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '인증 관련 사용자 상태가 변경될 때 증가하는 토큰 버전.';

CREATE TABLE workspace_join_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '워크스페이스 가입 요청 식별자.',
    workspace_id BIGINT UNSIGNED NOT NULL COMMENT '가입 대상 워크스페이스 ID. workspaces.id를 참조',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '가입 요청 사용자 ID. users.id를 참조',
    workspace_member_id BIGINT UNSIGNED NULL COMMENT '가입 완료 후 생성된 워크스페이스 구성원 ID. workspace_members.id를 참조',
    method VARCHAR(30) NOT NULL COMMENT '가입 요청 수단. INVITE_CODE 등은 애플리케이션 enum으로 관리',
    invite_code_snapshot CHAR(14) NOT NULL COMMENT '가입 요청 생성 시점에 사용된 초대 코드 값.',
    status VARCHAR(30) NOT NULL COMMENT '가입 요청의 현재 상태. PENDING, READY, APPROVED, JOINED, FAILED 등은 애플리케이션 enum으로 관리',
    requested_at BIGINT UNSIGNED NOT NULL COMMENT '가입 요청 생성 일시. Unix epoch seconds 기준.',
    ready_at BIGINT UNSIGNED NULL COMMENT '가입 요청이 승인 대기 가능 상태가 된 일시. Unix epoch seconds 기준.',
    approved_at BIGINT UNSIGNED NULL COMMENT '가입 요청 승인 일시. Unix epoch seconds 기준.',
    joined_at BIGINT UNSIGNED NULL COMMENT '워크스페이스 가입 완료 일시. Unix epoch seconds 기준.',
    failed_at BIGINT UNSIGNED NULL COMMENT '가입 요청 실패 상태 전이 일시. Unix epoch seconds 기준.',
    failure_code VARCHAR(100) NULL COMMENT '실패 분류 코드.',
    failure_message VARCHAR(500) NULL COMMENT '실패 상세 메시지.',
    created_at BIGINT UNSIGNED NOT NULL COMMENT '레코드 생성 일시. Unix epoch seconds 기준.',
    updated_at BIGINT UNSIGNED NOT NULL COMMENT '레코드 최종 수정 일시. Unix epoch seconds 기준.',

    PRIMARY KEY (id),
    KEY idx_workspace_join_requests_workspace_id (workspace_id),
    KEY idx_workspace_join_requests_user_id (user_id),
    KEY idx_workspace_join_requests_status (status),
    KEY idx_workspace_join_requests_requested_at (requested_at),

    CONSTRAINT fk_workspace_join_requests_workspace
        FOREIGN KEY (workspace_id)
            REFERENCES workspaces (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_workspace_join_requests_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_workspace_join_requests_workspace_member
        FOREIGN KEY (workspace_member_id)
            REFERENCES workspace_members (id)
            ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='워크스페이스 가입 요청 테이블. 가입 수단, 현재 상태, 최종 가입 결과를 관리.';

CREATE TABLE workspace_join_request_histories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '가입 요청 상태 전이 이력 식별자.',
    join_request_id BIGINT UNSIGNED NOT NULL COMMENT '가입 요청 ID. workspace_join_requests.id를 참조',
    from_status VARCHAR(30) NULL COMMENT '이전 상태. 최초 요청 이력에서는 NULL.',
    to_status VARCHAR(30) NOT NULL COMMENT '전이 이후 상태.',
    event VARCHAR(30) NOT NULL COMMENT '상태 전이 이벤트. 상태 머신 내부 감사/추적 용도로 사용',
    changed_by_user_id BIGINT UNSIGNED NULL COMMENT '상태 전이를 발생시킨 사용자 ID. users.id를 참조',
    changed_at BIGINT UNSIGNED NOT NULL COMMENT '상태 전이 발생 일시. Unix epoch seconds 기준.',

    PRIMARY KEY (id),
    KEY idx_workspace_join_request_histories_request_id (join_request_id),
    KEY idx_workspace_join_request_histories_changed_at (changed_at),

    CONSTRAINT fk_workspace_join_request_histories_request
        FOREIGN KEY (join_request_id)
            REFERENCES workspace_join_requests (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_workspace_join_request_histories_changed_by_user
        FOREIGN KEY (changed_by_user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='워크스페이스 가입 요청 상태 전이 이력 테이블. 상태 변화 감사/추적 정보를 관리.';
