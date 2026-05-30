ALTER TABLE users
	ADD COLUMN token_version BIGINT UNSIGNED NOT NULL DEFAULT 0
		COMMENT 'Increments when authentication-sensitive user state changes.'
		AFTER password_hash;
