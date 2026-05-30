package dev.runtime_lab.flowit.domain.file.storage;

public record ProfileImageFileContent(
	byte[] bytes
) {

	public long contentLength() {
		return bytes.length;
	}
}
