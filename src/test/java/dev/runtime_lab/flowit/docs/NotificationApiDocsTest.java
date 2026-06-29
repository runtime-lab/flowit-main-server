package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.notification.controller.NotificationController;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationActorResponse;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationActorType;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationAlertListResponse;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationAlertReadAllResponse;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationAlertResponse;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationAlertSeenResponse;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationAlertType;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationChangeResponse;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationLinkResponse;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationLinkType;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationScopeResponse;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationScopeType;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationSubjectResponse;
import dev.runtime_lab.flowit.domain.notification.dto.NotificationSubjectType;
import dev.runtime_lab.flowit.domain.notification.service.NotificationService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUserArgumentResolver;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiResponseBodyAdvice;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static dev.runtime_lab.flowit.docs.support.DocumentedTypes.numberParameter;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class NotificationApiDocsTest {

	private final NotificationService notificationService = mock(NotificationService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		mockMvc = MockMvcBuilders
			.standaloneSetup(new NotificationController(notificationService))
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
	void listNotifications() throws Exception {
		when(notificationService.alerts(any(CurrentUser.class), eq(0), eq(20)))
			.thenReturn(new NotificationAlertListResponse(List.of(alert(false)), 10L, 3L, 2L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(get("/v1/notifications")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.param("page", "0")
				.param("size", "20")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("notifications-list",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. ``Bearer {token}`` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				queryParameters(
					numberParameter("page").description("조회할 페이지 번호입니다. 0부터 시작합니다. 생략하거나 음수이면 0으로 처리합니다.").optional(),
					numberParameter("size").description("한 페이지에 조회할 알림 수입니다. 생략하면 20, 최소 1, 최대 100으로 처리합니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("현재 사용자에게 표시할 알림 목록과 카운트 정보입니다."),
					fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("현재 페이지의 알림 목록입니다."),
					fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("알림 식별자입니다."),
					fieldWithPath("data.items[].type").type(JsonFieldType.STRING).description("알림 유형입니다. link:notifications-enum-reference.html#notification-alert-type[NotificationAlertType]을 참고합니다."),
					fieldWithPath("data.items[].occurredAt").type(JsonFieldType.NUMBER).description("알림 원인이 된 이벤트 발생 시각입니다. Unix epoch seconds 기준입니다."),
					fieldWithPath("data.items[].scope").type(JsonFieldType.OBJECT).description("알림이 속한 범위입니다."),
					fieldWithPath("data.items[].scope.type").type(JsonFieldType.STRING).description("알림 범위 유형입니다. link:notifications-enum-reference.html#notification-scope-type[NotificationScopeType]을 참고합니다."),
					fieldWithPath("data.items[].scope.id").type(JsonFieldType.NUMBER).description("알림 범위 식별자입니다."),
					fieldWithPath("data.items[].scope.name").type(JsonFieldType.STRING).description("알림 범위 표시 이름 스냅샷입니다."),
					fieldWithPath("data.items[].actor").type(JsonFieldType.OBJECT).description("알림을 발생시킨 주체입니다."),
					fieldWithPath("data.items[].actor.type").type(JsonFieldType.STRING).description("알림 주체 유형입니다. link:notifications-enum-reference.html#notification-actor-type[NotificationActorType]을 참고합니다."),
					fieldWithPath("data.items[].actor.id").type(JsonFieldType.NUMBER).description("알림 주체 식별자입니다. 주체가 없으면 ``null``입니다.").optional(),
					fieldWithPath("data.items[].actor.name").type(JsonFieldType.STRING).description("알림 주체 표시 이름 스냅샷입니다. 주체가 없으면 ``null``입니다.").optional(),
					fieldWithPath("data.items[].actor.profileImageUrl").type(JsonFieldType.STRING).description("알림 주체 프로필 이미지 URL입니다. 없으면 ``null``입니다.").optional(),
					fieldWithPath("data.items[].subject").type(JsonFieldType.OBJECT).description("알림의 대상입니다."),
					fieldWithPath("data.items[].subject.type").type(JsonFieldType.STRING).description("알림 대상 유형입니다. link:notifications-enum-reference.html#notification-subject-type[NotificationSubjectType]을 참고합니다."),
					fieldWithPath("data.items[].subject.id").type(JsonFieldType.NUMBER).description("알림 대상 식별자입니다."),
					fieldWithPath("data.items[].subject.name").type(JsonFieldType.STRING).description("알림 대상 표시 이름 스냅샷입니다."),
					fieldWithPath("data.items[].changes").type(JsonFieldType.ARRAY).description("변경 내용 목록입니다. 변경 내용이 없으면 빈 배열입니다."),
					fieldWithPath("data.items[].changes[].element").type(JsonFieldType.STRING).description("변경된 요소 이름입니다."),
					fieldWithPath("data.items[].changes[].from").type(JsonFieldType.VARIES).description("변경 전 값입니다. 값 유형은 변경 요소에 따라 달라집니다.").optional(),
					fieldWithPath("data.items[].changes[].to").type(JsonFieldType.VARIES).description("변경 후 값입니다. 값 유형은 변경 요소에 따라 달라집니다.").optional(),
					fieldWithPath("data.items[].link").type(JsonFieldType.OBJECT).description("클라이언트 이동에 사용할 링크 힌트입니다."),
					fieldWithPath("data.items[].link.type").type(JsonFieldType.STRING).description("링크 유형입니다. link:notifications-enum-reference.html#notification-link-type[NotificationLinkType]을 참고합니다."),
					fieldWithPath("data.items[].link.workspaceId").type(JsonFieldType.NUMBER).description("링크가 가리키는 워크스페이스 식별자입니다. 링크 유형에 따라 ``null``일 수 있습니다.").optional(),
					fieldWithPath("data.items[].read").type(JsonFieldType.BOOLEAN).description("현재 사용자가 해당 알림을 읽었는지 여부입니다."),
					fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER).description("현재 사용자에게 표시 가능한 전체 알림 수입니다."),
					fieldWithPath("data.unreadCount").type(JsonFieldType.NUMBER).description("현재 사용자의 읽지 않은 알림 수입니다."),
					fieldWithPath("data.unseenCount").type(JsonFieldType.NUMBER).description("현재 사용자의 아직 확인하지 않은 알림 수입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}

	@Test
	void markNotificationsSeen() throws Exception {
		when(notificationService.seen(any(CurrentUser.class)))
			.thenReturn(new NotificationAlertSeenResponse(1782013300L, 2L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/notifications/seen")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("notifications-seen",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. ``Bearer {token}`` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("확인 처리 결과입니다."),
					fieldWithPath("data.seenAt").type(JsonFieldType.NUMBER).description("확인 처리 기준 시각입니다. Unix epoch seconds 기준입니다."),
					fieldWithPath("data.seenCount").type(JsonFieldType.NUMBER).description("이번 요청으로 확인 처리된 알림 수입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}

	@Test
	void markNotificationsReadAll() throws Exception {
		when(notificationService.readAll(any(CurrentUser.class)))
			.thenReturn(new NotificationAlertReadAllResponse(1782013400L, 3L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1003", "user@example.com", "nickname"), List.of())
		);

		mockMvc.perform(patch("/v1/notifications/read-all")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("notifications-read-all",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. ``Bearer {token}`` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("읽음 처리 결과입니다."),
					fieldWithPath("data.readAt").type(JsonFieldType.NUMBER).description("읽음 처리 기준 시각입니다. Unix epoch seconds 기준입니다."),
					fieldWithPath("data.readCount").type(JsonFieldType.NUMBER).description("이번 요청으로 읽음 처리된 알림 수입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
	}

	private NotificationAlertResponse alert(boolean read) {
		return new NotificationAlertResponse(
			1L,
			NotificationAlertType.WORKSPACE_MEMBER_REMOVED,
			1782013200L,
			new NotificationScopeResponse(NotificationScopeType.WORKSPACE, 12L, "Flowit"),
			new NotificationActorResponse(NotificationActorType.USER, 34L, "Actor", "/v1/users/34/profile-image"),
			new NotificationSubjectResponse(NotificationSubjectType.WORKSPACE_MEMBER, 55L, "Target"),
			List.of(new NotificationChangeResponse("MEMBERSHIP", "ACTIVE", "REMOVED")),
			new NotificationLinkResponse(NotificationLinkType.WORKSPACE_MEMBERS, 12L),
			read
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
