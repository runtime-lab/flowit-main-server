package dev.runtime_lab.flowit.domain.user.controller;

import dev.runtime_lab.flowit.domain.user.dto.JoinRequest;
import dev.runtime_lab.flowit.domain.user.dto.JoinResponse;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.exception.DuplicateActiveEmailException;
import dev.runtime_lab.flowit.domain.user.service.UserJoinService;
import dev.runtime_lab.flowit.global.security.password.InvalidPasswordPolicyException;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserJoinControllerTest {

	private final UserJoinService userJoinService = mock(UserJoinService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserJoinController(userJoinService))
			.setControllerAdvice(new ApiResponseBodyAdvice(), new GlobalExceptionHandler())
			.setValidator(validator)
			.build();
	}

	@Test
	void joinCreatesUser() throws Exception {
		String requestBody = """
			{
			  "email": "user@example.com",
			  "passwordPlain": "plainPassword",
			  "nickname": "nickname"
			}
			""";
		ArgumentCaptor<JoinRequest> requestCaptor = ArgumentCaptor.forClass(JoinRequest.class);

		when(userJoinService.join(any(JoinRequest.class))).thenReturn(
			new JoinResponse(1L, "user@example.com", "nickname", UserStatus.ACTIVE, 1779889000L)
		);

		mockMvc.perform(post("/v1/public/users/join")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/v1/public/users/1"))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.createdId").value(1L))
			.andExpect(jsonPath("$.extensions").isMap());

		verify(userJoinService).join(requestCaptor.capture());
		assertEquals("user@example.com", requestCaptor.getValue().email());
		assertEquals("plainPassword", requestCaptor.getValue().passwordPlain());
		assertEquals("nickname", requestCaptor.getValue().nickname());
	}

	@Test
	void joinRejectsInvalidRequest() throws Exception {
		String requestBody = """
			{
			  "email": "not-email",
			  "passwordPlain": "short",
			  "nickname": ""
			}
			""";

		mockMvc.perform(post("/v1/public/users/join")
			.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
			.andExpect(jsonPath("$.error.message").value("요청 값이 올바르지 않습니다."))
			.andExpect(jsonPath("$.extensions.fieldErrors").isArray());
	}

	@Test
	void joinReturnsConflictWhenActiveEmailAlreadyExists() throws Exception {
		String requestBody = """
			{
			  "email": "user@example.com",
			  "passwordPlain": "plainPassword",
			  "nickname": "nickname"
			}
			""";

		when(userJoinService.join(any(JoinRequest.class)))
			.thenThrow(new DuplicateActiveEmailException("user@example.com"));

		mockMvc.perform(post("/v1/public/users/join")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("USER_409_001"))
			.andExpect(jsonPath("$.error.message").value("Active user email already exists: user@example.com"))
			.andExpect(jsonPath("$.extensions").isMap());
	}

	@Test
	void joinReturnsBadRequestWhenPasswordContainsSpecialCharacter() throws Exception {
		String requestBody = """
			{
			  "email": "user@example.com",
			  "passwordPlain": "plainPassword!",
			  "nickname": "nickname"
			}
			""";

		when(userJoinService.join(any(JoinRequest.class)))
			.thenThrow(new InvalidPasswordPolicyException());

		mockMvc.perform(post("/v1/public/users/join")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_400_001"))
			.andExpect(jsonPath("$.error.message").value("Password must not contain special characters"))
			.andExpect(jsonPath("$.extensions").isMap());
	}
}
