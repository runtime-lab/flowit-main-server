package dev.runtime_lab.flowit.domain.file.storage;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalProfileImageStorageDirectoryResolverTest {

	@Test
	void resolveUsesConfiguredDirectoryWhenPresent() {
		Path configuredDirectory = Path.of("/custom/Flowit/main-server/profile-images");

		assertEquals(
			configuredDirectory,
			LocalProfileImageStorageDirectoryResolver.resolve(configuredDirectory.toString())
		);
	}

	@Test
	void resolveDefaultUsesMacApplicationSupportDirectory() {
		Path resolved = LocalProfileImageStorageDirectoryResolver.resolveDefault(
			"Mac OS X",
			"/Users/min",
			Map.of()
		);

		assertEquals(
			Path.of("/Users/min/Library/Application Support/Flowit/main-server/profile-images"),
			resolved
		);
	}

	@Test
	void resolveDefaultUsesWindowsLocalAppDataDirectory() {
		String resolved = LocalProfileImageStorageDirectoryResolver.resolveDefaultPathString(
			"Windows 11",
			"C:\\Users\\min",
			Map.of("LOCALAPPDATA", "C:\\Users\\min\\AppData\\Local")
		);

		assertEquals(
			"C:\\Users\\min\\AppData\\Local\\Flowit\\main-server\\profile-images",
			resolved
		);
	}

	@Test
	void resolveDefaultUsesXdgDataHomeOnLinux() {
		Path resolved = LocalProfileImageStorageDirectoryResolver.resolveDefault(
			"Linux",
			"/home/min",
			Map.of("XDG_DATA_HOME", "/home/min/.local/share")
		);

		assertEquals(
			Path.of("/home/min/.local/share/flowit/main-server/profile-images"),
			resolved
		);
	}

	@Test
	void resolveDefaultFallsBackToLocalShareOnLinux() {
		Path resolved = LocalProfileImageStorageDirectoryResolver.resolveDefault(
			"Linux",
			"/home/min",
			Map.of()
		);

		assertEquals(
			Path.of("/home/min/.local/share/flowit/main-server/profile-images"),
			resolved
		);
	}

	@Test
	void isDedicatedProfileImageDirectoryAcceptsOnlyFlowitProfileImageLeaf() {
		assertTrue(LocalProfileImageStorageDirectoryResolver.isDedicatedProfileImageDirectory(
			Path.of("/Users/min/Library/Application Support/Flowit/main-server/profile-images")
		));
		assertTrue(LocalProfileImageStorageDirectoryResolver.isDedicatedProfileImageDirectory(
			Path.of("/home/min/.local/share/flowit/main-server/profile-images")
		));
		assertFalse(LocalProfileImageStorageDirectoryResolver.isDedicatedProfileImageDirectory(
			Path.of("/Users/min/Library/Application Support/Flowit/main-server")
		));
		assertFalse(LocalProfileImageStorageDirectoryResolver.isDedicatedProfileImageDirectory(
			Path.of("/Users/min/Library/Application Support")
		));
	}
}
