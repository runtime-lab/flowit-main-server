package dev.runtime_lab.flowit.global.dev;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDevSampleRecordRegistry {

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	void ensureReady() {
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS local_dev_sample_records (
				scenario_key VARCHAR(100) NOT NULL,
				record_key VARCHAR(150) NOT NULL,
				entity_type VARCHAR(50) NOT NULL,
				entity_id BIGINT NOT NULL,
				created_at BIGINT NOT NULL,
				updated_at BIGINT NOT NULL,
				PRIMARY KEY (scenario_key, record_key),
				KEY idx_local_dev_sample_records_entity (entity_type, entity_id)
			)
			""");
	}

	Optional<Long> findEntityId(String recordKey, LocalDevSampleRecordType recordType) {
		List<Long> entityIds = jdbcTemplate.query(
			"""
			SELECT entity_id
			FROM local_dev_sample_records
			WHERE scenario_key = ?
				AND record_key = ?
				AND entity_type = ?
			""",
			(resultSet, rowNumber) -> resultSet.getLong("entity_id"),
			LocalDevScenarioSamples.SCENARIO_KEY,
			recordKey,
			recordType.name()
		);

		return entityIds.stream().findFirst();
	}

	void upsert(String recordKey, LocalDevSampleRecordType recordType, Long entityId) {
		long now = Instant.now(clock).getEpochSecond();
		jdbcTemplate.update(
			"""
			INSERT INTO local_dev_sample_records (
				scenario_key,
				record_key,
				entity_type,
				entity_id,
				created_at,
				updated_at
			)
			VALUES (?, ?, ?, ?, ?, ?)
			ON DUPLICATE KEY UPDATE
				entity_type = ?,
				entity_id = ?,
				updated_at = ?
			""",
			LocalDevScenarioSamples.SCENARIO_KEY,
			recordKey,
			recordType.name(),
			entityId,
			now,
			now,
			recordType.name(),
			entityId,
			now
		);
	}
}
