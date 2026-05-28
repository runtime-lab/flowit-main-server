package dev.runtime_lab.flowit.global.web.exception;

import dev.runtime_lab.flowit.domain.user.exception.DuplicateActiveEmailException;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidLoginCredentialsException;
import dev.runtime_lab.flowit.domain.auth.exception.InvalidRefreshTokenException;
import dev.runtime_lab.flowit.global.security.password.InvalidPasswordPolicyException;
import dev.runtime_lab.flowit.global.web.response.ApiError;
import dev.runtime_lab.flowit.global.web.response.ApiResponse;
import dev.runtime_lab.flowit.global.web.response.ResponseExtensionContext;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

	@ExceptionHandler(DuplicateActiveEmailException.class)
	public ResponseEntity<ApiResponse<Object>> handleDuplicateActiveEmail(DuplicateActiveEmailException exception) {
		ErrorCode errorCode = ErrorCode.USER_409_001;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(ApiError.from(errorCode, exception.getMessage())));
	}

	@ExceptionHandler(InvalidPasswordPolicyException.class)
	public ResponseEntity<ApiResponse<Object>> handleInvalidPasswordPolicy(InvalidPasswordPolicyException exception) {
		ErrorCode errorCode = ErrorCode.VALIDATION_400_001;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(ApiError.from(errorCode, exception.getMessage())));
	}

	@ExceptionHandler(InvalidLoginCredentialsException.class)
	public ResponseEntity<ApiResponse<Object>> handleInvalidLoginCredentials(InvalidLoginCredentialsException exception) {
		ErrorCode errorCode = ErrorCode.AUTH_401_001;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(ApiError.from(errorCode, exception.getMessage())));
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ApiResponse<Object>> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
		ErrorCode errorCode = ErrorCode.AUTH_401_001;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(ApiError.from(errorCode, exception.getMessage())));
	}

	private Map<String, String> fieldError(FieldError fieldError) {
		return Map.of(
			"field", fieldError.getField(),
			"message", fieldError.getDefaultMessage() == null ? "" : fieldError.getDefaultMessage()
		);
	}
}
