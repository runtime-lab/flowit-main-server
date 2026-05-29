package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.user.controller.UserJoinController;
import dev.runtime_lab.flowit.domain.user.dto.JoinRequest;
import dev.runtime_lab.flowit.domain.user.dto.JoinResponse;
import dev.runtime_lab.flowit.domain.user.entity.UserStatus;
import dev.runtime_lab.flowit.domain.user.exception.DuplicateActiveEmailException;
import dev.runtime_lab.flowit.domain.user.service.UserJoinService;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class UserJoinApiDocsTest {

	private final UserJoinService userJoinService = mock(UserJoinService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserJoinController(userJoinService))
			.setControllerAdvice(new ApiResponseBodyAdvice(), new GlobalExceptionHandler())
			.setValidator(validator)
			.apply(documentationConfiguration(restDocumentation))
			.build();
	}

	@Test
	void join() throws Exception {
		String requestBody = """
			{
			  "email": "user@example.com",
			  "passwordPlan": "plainPassword",
			  "nickname": "nickname"
			}
			""";

		when(userJoinService.join(any(JoinRequest.class))).thenReturn(
			new JoinResponse(1003L, "user@example.com", "nickname", UserStatus.ACTIVE, 1779889000L)
		);

		mockMvc.perform(post("/v1/public/users/join")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/v1/public/users/1003"))
			.andDo(document("users-join",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("요청 본문 미디어 타입입니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다."),
					headerWithName(HttpHeaders.LOCATION).description("생성된 유저 리소스 URI입니다.")
				),
				requestFields(
					fieldWithPath("email").type(JsonFieldType.STRING).description("가입 이메일입니다. 활성 계정 기준으로 유일해야 합니다."),
					fieldWithPath("passwordPlan").type(JsonFieldType.STRING).description("평문 비밀번호입니다. 서버에서 해시 처리 후 저장합니다."),
					fieldWithPath("nickname").type(JsonFieldType.STRING).description("유저 닉네임입니다. 유저 엔티티의 name으로 저장됩니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성 결과 데이터입니다."),
					fieldWithPath("data.createdId").type(JsonFieldType.NUMBER).description("생성된 유저 식별자입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}

	@Test
	void duplicateEmail() throws Exception {
		String requestBody = """
			{
			  "email": "user@example.com",
			  "passwordPlan": "plainPassword",
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
			.andDo(document("users-join-duplicate-email",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("email").type(JsonFieldType.STRING).description("가입 이메일입니다."),
					fieldWithPath("passwordPlan").type(JsonFieldType.STRING).description("평문 비밀번호입니다."),
					fieldWithPath("nickname").type(JsonFieldType.STRING).description("유저 닉네임입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("error").type(JsonFieldType.OBJECT).description("에러 정보입니다."),
					fieldWithPath("error.code").type(JsonFieldType.STRING).description("애플리케이션 에러 코드입니다."),
					fieldWithPath("error.message").type(JsonFieldType.STRING).description("에러 메시지입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}
}
