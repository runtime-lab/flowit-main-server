package dev.runtime_lab.flowit.domain.user.entity;

import dev.runtime_lab.flowit.domain.file.entity.FileMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(
	name = "users",
	indexes = {
		@Index(name = "idx_users_email", columnList = "email"),
		@Index(name = "idx_users_status", columnList = "status"),
		@Index(name = "idx_users_deleted_at", columnList = "deleted_at"),
		@Index(name = "idx_users_profile_image_file_id", columnList = "profile_image_file_id")
	}
)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Builder.Default
	@Column(name = "token_version", nullable = false)
	private Long tokenVersion = 0L;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "profile_image_file_id",
		unique = true,
		foreignKey = @ForeignKey(name = "fk_users_profile_image_file")
	)
	private FileMetadata profileImageFile;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	@Builder.Default
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "last_login_at")
	private Long lastLoginAt;

	@Column(name = "created_at", nullable = false)
	private Long createdAt;

	@Column(name = "updated_at", nullable = false)
	private Long updatedAt;

	@Column(name = "deleted_at")
	private Long deletedAt;

	public FileMetadata replaceProfileImageFile(FileMetadata newProfileImageFile, Long updatedAt) {
		FileMetadata oldProfileImageFile = this.profileImageFile;
		this.profileImageFile = newProfileImageFile;
		this.updatedAt = updatedAt;
		return oldProfileImageFile;
	}

	public void changeNickname(String nickname, Long updatedAt) {
		this.name = nickname;
		this.updatedAt = updatedAt;
	}

	public void changePassword(String passwordHash, Long updatedAt) {
		this.passwordHash = passwordHash;
		this.tokenVersion = nextTokenVersion();
		this.updatedAt = updatedAt;
	}

	private Long nextTokenVersion() {
		if (tokenVersion == null) {
			return 1L;
		}

		return tokenVersion + 1L;
	}
}
