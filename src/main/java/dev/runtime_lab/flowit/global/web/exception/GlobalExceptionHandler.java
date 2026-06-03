package dev.runtime_lab.flowit.global.web.exception;

import dev.runtime_lab.flowit.global.web.response.ApiError;
import dev.runtime_lab.flowit.global.web.response.ApiResponse;
import dev.runtime_lab.flowit.global.web.response.ResponseExtensionContext;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice(basePackages = "dev.runtime_lab.flowit")
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		ErrorCode errorCode = ErrorCode.VALIDATION_400_001;
		List<Map<String, String>> fieldErrors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::fieldError)
			.toList();

		ResponseExtensionContext.addProperty("fieldErrors", fieldErrors);

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(ApiError.from(errorCode)));
	}

	@ExceptionHandler({
		HttpMessageNotReadableException.class,
		MissingServletRequestParameterException.class,
		MissingServletRequestPartException.class,
		MethodArgumentTypeMismatchException.class,
		ConstraintViolationException.class
	})
	public ResponseEntity<ApiResponse<Object>> handleInvalidRequest(Exception exception) {
		ErrorCode errorCode = ErrorCode.VALIDATION_400_001;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(ApiError.from(errorCode)));
	}

	@ExceptionHandler(FlowitException.class)
	public ResponseEntity<ApiResponse<Object>> handleFlowitException(FlowitException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		if (errorCode.getHttpStatus().is5xxServerError()) {
			log.warn("Handled API error. code={}", errorCode.getCode(), exception);
		}

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(apiError(exception)));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
		ErrorCode errorCode = ErrorCode.FILE_400_001;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(ApiError.from(errorCode, "프로필 이미지 파일 크기가 허용 범위를 초과했습니다.")));
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiResponse<Object>> handleUnexpectedRuntime(RuntimeException exception) {
		log.error("Unexpected API error.", exception);

		ErrorCode errorCode = ErrorCode.INTERNAL_500_001;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(ApiError.from(errorCode)));
	}

	private ApiError apiError(FlowitException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		if (!exception.isExposeMessage()) {
			return ApiError.from(errorCode);
		}

		return ApiError.from(errorCode, exception.getMessage());
	}

	private Map<String, String> fieldError(FieldError fieldError) {
		return Map.of(
			"field", fieldError.getField(),
			"message", fieldError.getDefaultMessage() == null ? "" : fieldError.getDefaultMessage()
		);
	}
}
