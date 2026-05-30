package dev.runtime_lab.flowit.domain.file.storage;

import dev.runtime_lab.flowit.domain.file.exception.InvalidProfileImageException;
import dev.runtime_lab.flowit.domain.file.exception.ProfileImageStorageException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalProfileImageStorage {

	private static final String OWNERSHIP_MARKER = ".flowit-profile-images";
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/gif"
	);

	private final LocalProfileImageStorageProperties properties;

	public StoredProfileImageFile store(Long userId, MultipartFile file) {
		validateFile(file);

		String contentType = normalizeContentType(file.getContentType());
		String storageKey = storageKey(userId, extension(contentType));
		Path targetPath = resolveStorageKey(storageKey);
		Path tempPath = null;

		try {
			validateBaseDirectoryForWrite(baseDirectory());
			createDirectoriesUnderBase(targetPath.getParent());
			tempPath = Files.createTempFile(targetPath.getParent(), ".upload-", ".tmp");
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
			}

			ValidatedImage validatedImage = validateImageIntegrity(tempPath, contentType);

			Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			return new StoredProfileImageFile(
				storageKey,
				StringUtils.getFilename(file.getOriginalFilename()),
				validatedImage.contentType(),
				file.getSize(),
				validatedImage.width(),
				validatedImage.height()
			);
		}
		catch (InvalidProfileImageException exception) {
			deleteTempFile(tempPath);
			throw exception;
		}
		catch (IOException exception) {
			deleteTempFile(tempPath);
			throw new ProfileImageStorageException("프로필 이미지 파일 저장에 실패했습니다.", exception);
		}
	}

	public ProfileImageFileContent load(String storageKey) {
		try {
			Path path = resolveStorageKey(storageKey);
			Path baseDirectory = baseDirectory();
			if (!Files.exists(baseDirectory, LinkOption.NOFOLLOW_LINKS)) {
				throw new ProfileImageStorageException("프로필 이미지 저장 경로가 존재하지 않습니다.");
			}
			validateBaseDirectoryForCleanup(baseDirectory);
			validateStoragePathParents(path);
			if (Files.isSymbolicLink(path)) {
				throw new ProfileImageStorageException("프로필 이미지 파일은 symbolic link일 수 없습니다.");
			}
			if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
				throw new ProfileImageStorageException("프로필 이미지 파일을 찾을 수 없습니다.");
			}
			return new ProfileImageFileContent(Files.readAllBytes(path));
		}
		catch (ProfileImageStorageException exception) {
			throw exception;
		}
		catch (IOException exception) {
			throw new ProfileImageStorageException("프로필 이미지 파일 읽기에 실패했습니다.", exception);
		}
	}

	public void deleteIfExists(String storageKey) {
		try {
			Path path = resolveStorageKey(storageKey);
			validateStoragePathParents(path);
			Files.deleteIfExists(path);
			deleteEmptyParents(path.getParent());
		}
		catch (IOException | ProfileImageStorageException exception) {
			log.warn("Failed to delete local profile image file: {}", storageKey, exception);
		}
	}

	public void deleteOrphanFiles(Set<String> activeStorageKeys) {
		Path baseDirectory = baseDirectory();
		if (!Files.exists(baseDirectory)) {
			return;
		}
		validateBaseDirectoryForCleanup(baseDirectory);
		boolean shouldKeepOwnershipMarker = !activeStorageKeys.isEmpty();

		Set<Path> activePaths = new HashSet<>(activeStorageKeys.stream()
			.map(this::resolveStorageKey)
			.collect(java.util.stream.Collectors.toSet()));
		if (shouldKeepOwnershipMarker) {
			activePaths.add(ownershipMarkerPath(baseDirectory));
		}

		try (java.util.stream.Stream<Path> paths = Files.walk(baseDirectory)) {
			paths.sorted(Comparator.reverseOrder())
				.forEach(path -> deleteIfOrphan(path, activePaths));
		}
		catch (IOException exception) {
			throw new ProfileImageStorageException("프로필 이미지 로컬 파일 정리에 실패했습니다.", exception);
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidProfileImageException("프로필 이미지 파일은 비어 있을 수 없습니다.");
		}
		if (file.getSize() > properties.maxSize().toBytes()) {
			throw new InvalidProfileImageException("프로필 이미지 파일 크기가 허용 범위를 초과했습니다.");
		}

		String contentType = normalizeContentType(file.getContentType());
		if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new InvalidProfileImageException("지원하지 않는 프로필 이미지 MIME type입니다.");
		}
	}

	private String normalizeContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			throw new InvalidProfileImageException("프로필 이미지 MIME type이 필요합니다.");
		}
		return contentType.toLowerCase(Locale.ROOT).trim();
	}

	private String storageKey(Long userId, String extension) {
		return "users/%d/%s%s".formatted(userId, UUID.randomUUID(), extension);
	}

	private String extension(String contentType) {
		return switch (contentType) {
			case "image/jpeg" -> ".jpg";
			case "image/png" -> ".png";
			case "image/gif" -> ".gif";
			default -> throw new InvalidProfileImageException("지원하지 않는 프로필 이미지 MIME type입니다.");
		};
	}

	private ValidatedImage validateImageIntegrity(Path imagePath, String expectedContentType) {
		try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(imagePath.toFile())) {
			if (imageInputStream == null) {
				throw new InvalidProfileImageException("프로필 이미지 파일을 읽을 수 없습니다.");
			}

			Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
			if (!readers.hasNext()) {
				throw new InvalidProfileImageException("지원하지 않는 이미지 파일입니다.");
			}

			ImageReader reader = readers.next();
			try {
				String detectedContentType = contentTypeFromFormat(reader.getFormatName());
				if (!expectedContentType.equals(detectedContentType)) {
					throw new InvalidProfileImageException("요청 MIME type과 실제 이미지 형식이 일치하지 않습니다.");
				}

				reader.setInput(imageInputStream, false, false);
				int imageCount = reader.getNumImages(true);
				if (imageCount <= 0) {
					throw new InvalidProfileImageException("프로필 이미지 파일에 이미지 데이터가 없습니다.");
				}

				BufferedImage firstImage = null;
				for (int index = 0; index < imageCount; index++) {
					BufferedImage image = reader.read(index);
					if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
						throw new InvalidProfileImageException("프로필 이미지 파일 무결성 검사에 실패했습니다.");
					}
					if (firstImage == null) {
						firstImage = image;
					}
				}

				return new ValidatedImage(
					detectedContentType,
					firstImage.getWidth(),
					firstImage.getHeight()
				);
			}
			finally {
				reader.dispose();
			}
		}
		catch (InvalidProfileImageException exception) {
			throw exception;
		}
		catch (IOException | RuntimeException exception) {
			throw new InvalidProfileImageException("프로필 이미지 파일 무결성 검사에 실패했습니다.");
		}
	}

	private String contentTypeFromFormat(String formatName) {
		return switch (formatName.toLowerCase(Locale.ROOT)) {
			case "jpeg", "jpg" -> "image/jpeg";
			case "png" -> "image/png";
			case "gif" -> "image/gif";
			default -> throw new InvalidProfileImageException("지원하지 않는 이미지 파일입니다.");
		};
	}

	private Path baseDirectory() {
		Path baseDirectory = LocalProfileImageStorageDirectoryResolver.resolve(properties.directory())
			.toAbsolutePath()
			.normalize();
		if (baseDirectory.getRoot() != null && baseDirectory.equals(baseDirectory.getRoot())) {
			throw new ProfileImageStorageException("프로필 이미지 저장 경로로 루트 디렉터리는 사용할 수 없습니다.");
		}
		if (!LocalProfileImageStorageDirectoryResolver.isDedicatedProfileImageDirectory(baseDirectory)) {
			throw new ProfileImageStorageException(
				"프로필 이미지 저장 경로는 Flowit 전용 main-server/profile-images 디렉터리여야 합니다."
			);
		}
		return baseDirectory;
	}

	private Path resolveStorageKey(String storageKey) {
		if (storageKey == null || storageKey.isBlank()) {
			throw new ProfileImageStorageException("프로필 이미지 storage key가 비어 있습니다.");
		}
		Path baseDirectory = baseDirectory();
		Path resolvedPath = baseDirectory.resolve(storageKey).normalize();
		if (!resolvedPath.startsWith(baseDirectory)) {
			throw new ProfileImageStorageException("프로필 이미지 storage key가 저장소 경로를 벗어났습니다.");
		}
		return resolvedPath;
	}

	private void validateBaseDirectoryForWrite(Path baseDirectory) throws IOException {
		if (Files.exists(baseDirectory)) {
			validateBaseDirectoryForCleanup(baseDirectory);
			return;
		}
		Files.createDirectories(baseDirectory);
		createOwnershipMarker(baseDirectory);
	}

	private void validateBaseDirectoryForCleanup(Path baseDirectory) {
		if (Files.isSymbolicLink(baseDirectory)) {
			throw new ProfileImageStorageException("프로필 이미지 저장 경로는 symbolic link일 수 없습니다.");
		}
		if (!Files.isDirectory(baseDirectory, LinkOption.NOFOLLOW_LINKS)) {
			throw new ProfileImageStorageException("프로필 이미지 저장 경로가 디렉터리가 아닙니다.");
		}
		validateOwnershipMarker(baseDirectory);
	}

	private void createDirectoriesUnderBase(Path directory) throws IOException {
		Path baseDirectory = baseDirectory();
		Path normalizedDirectory = directory.toAbsolutePath().normalize();
		if (!normalizedDirectory.startsWith(baseDirectory)) {
			throw new ProfileImageStorageException("프로필 이미지 디렉터리가 저장소 경로를 벗어났습니다.");
		}

		Path current = baseDirectory;
		for (Path segment : baseDirectory.relativize(normalizedDirectory)) {
			current = current.resolve(segment);
			if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
				validateExistingStorageDirectory(current);
				continue;
			}
			Files.createDirectory(current);
		}
	}

	private void validateStoragePathParents(Path path) {
		Path baseDirectory = baseDirectory();
		Path parent = path.toAbsolutePath().normalize().getParent();
		if (parent == null || !parent.startsWith(baseDirectory)) {
			throw new ProfileImageStorageException("프로필 이미지 파일 부모 경로가 저장소 경로를 벗어났습니다.");
		}

		Path current = baseDirectory;
		for (Path segment : baseDirectory.relativize(parent)) {
			current = current.resolve(segment);
			if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
				return;
			}
			validateExistingStorageDirectory(current);
		}
	}

	private void validateExistingStorageDirectory(Path directory) {
		if (Files.isSymbolicLink(directory)) {
			throw new ProfileImageStorageException("프로필 이미지 저장소 하위 경로는 symbolic link일 수 없습니다.");
		}
		if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
			throw new ProfileImageStorageException("프로필 이미지 저장소 하위 경로가 디렉터리가 아닙니다.");
		}
	}

	private void validateOwnershipMarker(Path baseDirectory) {
		Path markerPath = ownershipMarkerPath(baseDirectory);
		if (Files.exists(markerPath, LinkOption.NOFOLLOW_LINKS)) {
			if (Files.isSymbolicLink(markerPath) || !Files.isRegularFile(markerPath, LinkOption.NOFOLLOW_LINKS)) {
				throw new ProfileImageStorageException("프로필 이미지 저장 경로의 ownership marker가 올바르지 않습니다.");
			}
			return;
		}
		if (isDirectoryEmpty(baseDirectory)) {
			try {
				createOwnershipMarker(baseDirectory);
				return;
			}
			catch (IOException exception) {
				throw new ProfileImageStorageException("프로필 이미지 저장 경로 ownership marker 생성에 실패했습니다.", exception);
			}
		}
		throw new ProfileImageStorageException("프로필 이미지 저장 경로가 Flowit 전용 디렉터리가 아닙니다.");
	}

	private Path ownershipMarkerPath(Path baseDirectory) {
		return baseDirectory.resolve(OWNERSHIP_MARKER).toAbsolutePath().normalize();
	}

	private void createOwnershipMarker(Path baseDirectory) throws IOException {
		Path markerPath = ownershipMarkerPath(baseDirectory);
		if (Files.exists(markerPath, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		Files.writeString(
			markerPath,
			"Flowit local profile image storage\n",
			StandardCharsets.UTF_8,
			StandardOpenOption.CREATE_NEW
		);
	}

	private void deleteIfOrphan(Path path, Set<Path> activePaths) {
		Path baseDirectory = baseDirectory();
		if (path.equals(baseDirectory)) {
			deleteDirectoryIfEmpty(path);
			return;
		}
		if (!path.normalize().startsWith(baseDirectory)) {
			throw new ProfileImageStorageException("프로필 이미지 정리 대상이 저장소 경로를 벗어났습니다.");
		}

		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path)) {
			deleteDirectoryIfEmpty(path);
			return;
		}

		if (!activePaths.contains(path.toAbsolutePath().normalize())) {
			try {
				Files.deleteIfExists(path);
			}
			catch (IOException exception) {
				throw new ProfileImageStorageException("프로필 이미지 orphan 파일 삭제에 실패했습니다.", exception);
			}
		}
	}

	private void deleteDirectoryIfEmpty(Path directory) {
		try {
			if (isDirectoryEmpty(directory)) {
				Files.deleteIfExists(directory);
			}
		}
		catch (IOException exception) {
			throw new ProfileImageStorageException("프로필 이미지 빈 디렉터리 삭제에 실패했습니다.", exception);
		}
	}

	private boolean isDirectoryEmpty(Path directory) {
		try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
			return !entries.iterator().hasNext();
		}
		catch (IOException exception) {
			throw new ProfileImageStorageException("프로필 이미지 디렉터리 확인에 실패했습니다.", exception);
		}
	}

	private void deleteEmptyParents(Path directory) throws IOException {
		Path baseDirectory = baseDirectory();
		Path current = directory;
		while (current != null && current.startsWith(baseDirectory)) {
			if (Files.isSymbolicLink(current) || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
				return;
			}
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(current)) {
				if (entries.iterator().hasNext()) {
					return;
				}
			}
			Files.deleteIfExists(current);
			if (current.equals(baseDirectory)) {
				return;
			}
			current = current.getParent();
		}
	}

	private void deleteTempFile(Path tempPath) {
		if (tempPath == null) {
			return;
		}
		try {
			Files.deleteIfExists(tempPath);
		}
		catch (IOException exception) {
			log.warn("Failed to delete temporary profile image file: {}", tempPath, exception);
		}
	}

	private record ValidatedImage(
		String contentType,
		int width,
		int height
	) {
	}
}
