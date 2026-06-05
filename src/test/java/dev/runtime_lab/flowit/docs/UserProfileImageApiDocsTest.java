package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.user.controller.UserController;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageContentResponse;
import dev.runtime_lab.flowit.domain.user.dto.UserProfileImageUpdateResponse;
import dev.runtime_lab.flowit.domain.user.service.UserMeService;
import dev.runtime_lab.flowit.domain.user.service.UserPasswordUpdateService;
import dev.runtime_lab.flowit.domain.user.service.UserProfileService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUserArgumentResolver;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.security.jwt.RefreshTokenCookieService;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.operation.OperationRequest;
import org.springframework.restdocs.operation.OperationResponse;
import org.springframework.restdocs.operation.OperationRequestFactory;
import org.springframework.restdocs.operation.OperationResponseFactory;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static dev.runtime_lab.flowit.docs.support.ResponseFieldStability.experimental;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class UserProfileImageApiDocsTest {

	private final UserMeService userMeService = mock(UserMeService.class);
	private final UserProfileService userProfileService = mock(UserProfileService.class);
	private final UserPasswordUpdateService userPasswordUpdateService = mock(UserPasswordUpdateService.class);
	private final RefreshTokenCookieService refreshTokenCookieService = new RefreshTokenCookieService(
		new JwtProperties(
			"flowit-test",
			Duration.ofMinutes(15),
			Duration.ofDays(14),
			"flowit_refresh_token",
			"/v1/public/auth",
			"Lax",
			false,
			null,
			null,
			null,
			null
		)
	);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(
				userMeService,
				userProfileService,
				userPasswordUpdateService,
				refreshTokenCookieService
			))
			.setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
			.setControllerAdvice(new ApiResponseBodyAdvice(), new GlobalExceptionHandler())
			.apply(documentationConfiguration(restDocumentation))
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void replaceProfileImage() throws Exception {
		when(userProfileService.replaceProfileImage(any(CurrentUser.class), any()))
			.thenReturn(new UserProfileImageUpdateResponse(3001L, "image/png", 68L, 1, 1));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(multipart("/v1/users/me/profile-image")
				.file(profileImage())
				.with(request -> {
					request.setMethod("PUT");
					return request;
				})
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("users-profile-image",
				preprocessRequest(omitMultipartBinaryContent()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. ``Bearer {token}`` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.CONTENT_TYPE).description("요청 본문 미디어 타입입니다. ``multipart/form-data``를 사용합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				requestParts(
					partWithName("file").description("교체할 프로필 이미지 파일입니다. ``image/jpeg``, ``image/png``, ``image/gif``를 지원합니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("프로필 이미지 교체 결과 데이터입니다."),
					fieldWithPath("data.fileId").type(JsonFieldType.NUMBER).description("새로 저장된 프로필 이미지 파일 식별자입니다."),
					fieldWithPath("data.contentType").type(JsonFieldType.STRING).description("저장된 이미지 MIME type입니다.").attributes(experimental()),
					fieldWithPath("data.sizeBytes").type(JsonFieldType.NUMBER).description("저장된 이미지 파일 크기입니다. bytes 기준입니다."),
					fieldWithPath("data.width").type(JsonFieldType.NUMBER).description("저장된 이미지 너비입니다. pixels 기준입니다.").attributes(experimental()),
					fieldWithPath("data.height").type(JsonFieldType.NUMBER).description("저장된 이미지 높이입니다. pixels 기준입니다.").attributes(experimental()),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}

	@Test
	void getProfileImageContent() throws Exception {
		when(userProfileService.getProfileImage(any(CurrentUser.class)))
			.thenReturn(new UserProfileImageContentResponse("image/png", pngBytes()));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/users/me/profile-image")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.accept(MediaType.IMAGE_PNG))
			.andExpect(status().isOk())
			.andDo(document("users-profile-image-content",
				preprocessRequest(prettyPrint()),
				preprocessResponse(omitBinaryResponseContent()),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. ``Bearer {token}`` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 이미지 응답 미디어 타입입니다. ``image/jpeg``, ``image/png``, ``image/gif``, ``*/*``를 사용할 수 있습니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("전송되는 프로필 이미지의 MIME type입니다."),
					headerWithName(HttpHeaders.CONTENT_LENGTH).description("전송되는 이미지 파일 크기입니다. bytes 기준입니다."),
					headerWithName(HttpHeaders.CACHE_CONTROL).description("프로필 이미지 변경 직후 오래된 이미지가 재사용되지 않도록 ``no-store``로 응답합니다.")
				)
			));
	}

	private OperationPreprocessor omitMultipartBinaryContent() {
		return new OperationPreprocessor() {

			@Override
			public OperationRequest preprocess(OperationRequest request) {
				String boundary = "6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm";
				String sanitizedContent = """
					--%s
					Content-Disposition: form-data; name=file; filename=avatar.png
					Content-Type: image/png

					<binary image content omitted>
					--%s--
					""".formatted(boundary, boundary)
					.stripIndent()
					.replace("\n", "\r\n");
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.AUTHORIZATION, request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
				headers.setAccept(request.getHeaders().getAccept());
				headers.set(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);

				return new OperationRequestFactory().create(
					request.getUri(),
					request.getMethod(),
					sanitizedContent.getBytes(StandardCharsets.UTF_8),
					headers,
					request.getParts(),
					request.getCookies()
				);
			}

			@Override
			public OperationResponse preprocess(OperationResponse response) {
				return response;
			}
		};
	}

	private OperationPreprocessor omitBinaryResponseContent() {
		return new OperationPreprocessor() {

			@Override
			public OperationRequest preprocess(OperationRequest request) {
				return request;
			}

			@Override
			public OperationResponse preprocess(OperationResponse response) {
				byte[] sanitizedContent = omittedBinaryContent(response.getHeaders().getContentLength());
				HttpHeaders headers = new HttpHeaders();
				headers.putAll(response.getHeaders());

				return new OperationResponseFactory().create(
					response.getStatus(),
					headers,
					sanitizedContent,
					response.getCookies()
				);
			}
		};
	}

	private byte[] omittedBinaryContent(long contentLength) {
		String markerText = "<binary image content omitted; original length %d bytes>".formatted(contentLength);
		byte[] marker = markerText.getBytes(StandardCharsets.UTF_8);
		if (contentLength <= marker.length || contentLength > Integer.MAX_VALUE) {
			return marker;
		}

		byte[] paddedContent = new byte[(int) contentLength];
		java.util.Arrays.fill(paddedContent, (byte) '.');
		System.arraycopy(marker, 0, paddedContent, 0, marker.length);
		return paddedContent;
	}

	private MockMultipartFile profileImage() {
		return new MockMultipartFile("file", "avatar.png", "image/png", pngBytes());
	}

	private byte[] pngBytes() {
		return Base64.getDecoder().decode(
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
		);
	}

	private Jwt jwt(String subject, String email, String name) {
		return Jwt.withTokenValue("token")
			.header("alg", "none")
			.subject(subject)
			.claim("email", email)
			.claim("name", name)
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
			.build();
	}
}
