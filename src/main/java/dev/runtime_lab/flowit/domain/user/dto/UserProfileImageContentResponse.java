package dev.runtime_lab.flowit.domain.user.dto;

public record UserProfileImageContentResponse(
	String contentType,
	byte[] bytes
) {

	public long contentLength() {
		return bytes.length;
	}
}
