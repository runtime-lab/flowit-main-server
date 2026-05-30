package dev.runtime_lab.flowit.domain.file.storage;

import dev.runtime_lab.flowit.domain.file.exception.InvalidProfileImageException;
import dev.runtime_lab.flowit.domain.file.exception.ProfileImageStorageException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalProfileImageStorageTest {

	@TempDir
	private Path tempDir;

	@Test
	void storeWritesProfileImageUnderConfiguredDirectory() throws Exception {
		Path baseDirectory = storageDirectory();
		LocalProfileImageStorage storage = storage(baseDirectory);

		StoredProfileImageFile storedFile = storage.store(1L, pngFile());

		assertTrue(Files.exists(baseDirectory.resolve(storedFile.storageKey())));
		assertTrue(Files.exists(baseDirectory.resolve(".flowit-profile-images")));
		assertTrue(storedFile.storageKey().startsWith("users/1/"));
		assertEquals("image/png", storedFile.contentType());
		assertEquals(1, storedFile.width());
		assertEquals(1, storedFile.height());
	}

	@Test
	void loadReturnsStoredProfileImageContent() throws Exception {
		LocalProfileImageStorage storage = storage(storageDirectory());
		MockMultipartFile file = pngFile();
		StoredProfileImageFile storedFile = storage.store(1L, file);

		ProfileImageFileContent content = storage.load(storedFile.storageKey());

		assertArrayEquals(file.getBytes(), content.bytes());
		assertEquals(file.getSize(), content.contentLength());
	}

	@Test
	void loadRejectsMissingPhysicalFile() {
		LocalProfileImageStorage storage = storage(storageDirectory());

		assertThrows(ProfileImageStorageException.class, () -> storage.load("users/1/missing.png"));
	}

	@Test
	void loadRejectsStorageKeyOutsideBaseDirectory() {
		LocalProfileImageStorage storage = storage(storageDirectory());

		assertThrows(ProfileImageStorageException.class, () -> storage.load("../outside.png"));
	}

	@Test
	void storeRejectsNonImageFile() {
		LocalProfileImageStorage storage = storage(storageDirectory());
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"avatar.txt",
			"text/plain",
			"not-image".getBytes(java.nio.charset.StandardCharsets.UTF_8)
		);

		assertThrows(InvalidProfileImageException.class, () -> storage.store(1L, file));
	}

	@Test
	void storeRejectsMismatchedMimeTypeAndActualImageFormat() throws Exception {
		LocalProfileImageStorage storage = storage(storageDirectory());
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"avatar.png",
			"image/png",
			imageBytes("jpg")
		);

		assertThrows(InvalidProfileImageException.class, () -> storage.store(1L, file));
	}

	@Test
	void storeRejectsCorruptedImageContent() {
		LocalProfileImageStorage storage = storage(storageDirectory());
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"avatar.png",
			"image/png",
			new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}
		);

		assertThrows(InvalidProfileImageException.class, () -> storage.store(1L, file));
	}

	@Test
	void deleteOrphanFilesKeepsActiveFilesAndDeletesOnlyFilesUnderBaseDirectory() throws Exception {
		Path baseDirectory = storageDirectory();
		Path activeFile = baseDirectory.resolve("users/1/active.png");
		Path orphanFile = baseDirectory.resolve("users/2/orphan.png");
		Path outsideFile = tempDir.resolve("outside.png");
		Files.createDirectories(activeFile.getParent());
		Files.createDirectories(orphanFile.getParent());
		Files.writeString(baseDirectory.resolve(".flowit-profile-images"), "marker", StandardCharsets.UTF_8);
		Files.write(activeFile, new byte[] {1});
		Files.write(orphanFile, new byte[] {2});
		Files.write(outsideFile, new byte[] {3});

		storage(baseDirectory).deleteOrphanFiles(Set.of("users/1/active.png"));

		assertTrue(Files.exists(activeFile));
		assertFalse(Files.exists(orphanFile));
		assertTrue(Files.exists(baseDirectory.resolve(".flowit-profile-images")));
		assertTrue(Files.exists(outsideFile));
	}

	@Test
	void deleteOrphanFilesDeletesBaseDirectoryWhenNoRowsRemain() throws Exception {
		Path baseDirectory = storageDirectory();
		Path orphanFile = baseDirectory.resolve("users/1/orphan.png");
		Files.createDirectories(orphanFile.getParent());
		Files.writeString(baseDirectory.resolve(".flowit-profile-images"), "marker", StandardCharsets.UTF_8);
		Files.write(orphanFile, new byte[] {1});

		storage(baseDirectory).deleteOrphanFiles(Set.of());

		assertFalse(Files.exists(baseDirectory));
	}

	@Test
	void deleteOrphanFilesRejectsNonOwnedNonEmptyDirectory() throws Exception {
		Path baseDirectory = storageDirectory();
		Path unrelatedFile = baseDirectory.resolve("unrelated.txt");
		Files.createDirectories(baseDirectory);
		Files.writeString(unrelatedFile, "do-not-delete", StandardCharsets.UTF_8);

		assertThrows(
			ProfileImageStorageException.class,
			() -> storage(baseDirectory).deleteOrphanFiles(Set.of())
		);
		assertTrue(Files.exists(unrelatedFile));
	}

	@Test
	void storeRejectsDirectoryThatIsNotDedicatedProfileImageLeaf() {
		Path broadDirectory = tempDir.resolve("Flowit").resolve("main-server");
		LocalProfileImageStorage storage = storage(broadDirectory);

		assertThrows(ProfileImageStorageException.class, () -> storage.store(1L, pngFile()));
	}

	private LocalProfileImageStorage storage(Path baseDirectory) {
		return new LocalProfileImageStorage(
			new LocalProfileImageStorageProperties(baseDirectory.toString(), DataSize.ofMegabytes(1), true)
		);
	}

	private Path storageDirectory() {
		return tempDir.resolve("Flowit").resolve("main-server").resolve("profile-images");
	}

	private MockMultipartFile pngFile() throws Exception {
		return new MockMultipartFile("file", "avatar.png", "image/png", pngBytes());
	}

	private byte[] pngBytes() throws Exception {
		return imageBytes("png");
	}

	private byte[] imageBytes(String formatName) throws Exception {
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, formatName, outputStream);
		return outputStream.toByteArray();
	}
}
