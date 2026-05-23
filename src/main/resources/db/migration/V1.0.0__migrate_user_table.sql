CREATE TABLE users (
                       id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '회원 식별자. UUID 대신 MySQL AUTO_INCREMENT 기반 시퀀스 ID를 사용',
                       email VARCHAR(255) NOT NULL COMMENT '이메일 계정. 로그인 식별자로 사용되며 활성 계정 기준으로 유일',
                       password_hash VARCHAR(255) NOT NULL COMMENT '비밀번호 해시값',
                       name VARCHAR(100) NOT NULL COMMENT '사용자 이름 또는 닉네임',
                       profile_image_file_id BIGINT UNSIGNED NULL COMMENT '프로필 이미지 파일 ID. files.id를 참조',
                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '회원 상태. ACTIVE, LOCKED, WITHDRAWN 등은 애플리케이션 enum으로 관리',
                       last_login_at BIGINT UNSIGNED NULL COMMENT '마지막 로그인 일시. Unix epoch seconds 기준.',
                       created_at BIGINT UNSIGNED NOT NULL COMMENT '회원 생성 일시. Unix epoch seconds 기준.',
                       updated_at BIGINT UNSIGNED NOT NULL COMMENT '회원 정보 최종 수정 일시. Unix epoch seconds 기준.',
                       deleted_at BIGINT UNSIGNED NULL COMMENT '회원 소프트 삭제 또는 탈퇴 처리 일시. Unix epoch seconds 기준.',

                       active_email VARCHAR(255)
                           GENERATED ALWAYS AS (
                               CASE
                                   WHEN deleted_at IS NULL THEN email
                                   ELSE NULL
                                   END
                               ) STORED COMMENT '활성 계정 이메일 유니크 제약을 위한 생성 컬럼.',

                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_active_email (active_email),
                       KEY idx_users_email (email),
                       KEY idx_users_status (status),
                       KEY idx_users_deleted_at (deleted_at),
                       KEY idx_users_profile_image_file_id (profile_image_file_id),

                       CONSTRAINT fk_users_profile_image_file
                           FOREIGN KEY (profile_image_file_id)
                               REFERENCES files (id)
                               ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='서비스 사용자 계정 테이블. 로그인, 프로필, 계정 상태를 관리.';

CREATE TABLE files (
                       id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '파일 식별자.',
                       storage_key VARCHAR(500) NOT NULL COMMENT 'Object storage 내부 파일 key 또는 경로.',
                       original_filename VARCHAR(255) NULL COMMENT '업로드 당시 원본 파일명.',
                       content_type VARCHAR(100) NOT NULL COMMENT '파일 MIME type.',
                       size_bytes BIGINT UNSIGNED NOT NULL COMMENT '파일 크기. bytes 기준.',
                       width INT UNSIGNED NULL COMMENT '이미지 너비. 이미지 파일이 아닌 경우 NULL.',
                       height INT UNSIGNED NULL COMMENT '이미지 높이. 이미지 파일이 아닌 경우 NULL.',
                       created_at BIGINT UNSIGNED NOT NULL COMMENT '파일 메타데이터 생성 일시. Unix epoch milliseconds 기준.',
                       updated_at BIGINT UNSIGNED NOT NULL COMMENT '파일 메타데이터 최종 수정 일시. Unix epoch milliseconds 기준.',
                       deleted_at BIGINT UNSIGNED NULL COMMENT '파일 소프트 삭제 일시. Unix epoch milliseconds 기준.',

                       PRIMARY KEY (id),
                       UNIQUE KEY uk_files_storage_key (storage_key),
                       KEY idx_files_deleted_at (deleted_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='업로드 파일 메타데이터 테이블.';