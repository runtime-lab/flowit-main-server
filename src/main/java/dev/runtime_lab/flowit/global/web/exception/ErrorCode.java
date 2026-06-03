package dev.runtime_lab.flowit.global.web.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	VALIDATION_400_001(
		HttpStatus.BAD_REQUEST,
		"요청 값이 올바르지 않습니다.",
		"요청 본문 또는 파라미터 검증에 실패했습니다."
	),
	AUTH_401_001(
		HttpStatus.UNAUTHORIZED,
		"인증이 필요하거나 인증에 실패했습니다.",
		"유효한 인증 정보가 제공되지 않았거나 제공된 인증 정보를 검증할 수 없습니다."
	),
	AUTH_403_001(
		HttpStatus.FORBIDDEN,
		"접근 권한이 없습니다.",
		"인증된 사용자가 해당 리소스에 접근할 권한을 가지고 있지 않습니다."
	),
	USER_409_001(
		HttpStatus.CONFLICT,
		"이미 가입된 이메일입니다.",
		"활성 사용자 중 동일한 이메일이 이미 존재합니다."
	),
	WORKSPACE_404_001(
		HttpStatus.NOT_FOUND,
		"워크스페이스를 찾을 수 없습니다.",
		"요청한 워크스페이스가 존재하지 않거나 삭제되었습니다."
	),
	WORKSPACE_MEMBER_404_001(
		HttpStatus.NOT_FOUND,
		"워크스페이스 멤버를 찾을 수 없습니다.",
		"요청한 사용자가 해당 워크스페이스의 활성 멤버가 아니거나 이미 제거되었습니다."
	),
	WORKSPACE_500_001(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"워크스페이스 처리에 실패했습니다.",
		"워크스페이스 초대 코드 생성 등 워크스페이스 내부 처리 중 오류가 발생했습니다."
	),
	FILE_400_001(
		HttpStatus.BAD_REQUEST,
		"프로필 이미지 파일이 올바르지 않습니다.",
		"프로필 이미지 파일이 비어 있거나 지원하지 않는 이미지 형식입니다."
	),
	FILE_404_001(
		HttpStatus.NOT_FOUND,
		"프로필 이미지를 찾을 수 없습니다.",
		"현재 사용자에게 등록된 프로필 이미지가 없습니다."
	),
	FILE_500_001(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"프로필 이미지 파일 처리에 실패했습니다.",
		"프로필 이미지 로컬 파일 저장, 조회 또는 정리 중 오류가 발생했습니다."
	),
	INTERNAL_500_001(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"서버 내부 오류가 발생했습니다.",
		"예상하지 못한 서버 내부 오류가 발생했습니다."
	);

	private final HttpStatus httpStatus;
	private final String message;
	private final String description;

	public String getCode() {
		return name();
	}
}
