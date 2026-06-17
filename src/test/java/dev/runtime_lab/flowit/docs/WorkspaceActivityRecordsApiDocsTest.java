package dev.runtime_lab.flowit.docs;

import dev.runtime_lab.flowit.domain.activity.dto.ActivityActorResponse;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityChangeElement;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityChangeResponse;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordAction;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordDomain;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordListQuery;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityRecordResponse;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityTargetResponse;
import dev.runtime_lab.flowit.domain.activity.dto.ActivityTargetType;
import dev.runtime_lab.flowit.domain.activity.service.WorkspaceActivityService;
import dev.runtime_lab.flowit.domain.workspace.controller.WorkspaceController;
import dev.runtime_lab.flowit.domain.workspace.service.WorkspaceService;
import dev.runtime_lab.flowit.global.security.authentication.AuthenticatedUserArgumentResolver;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.exception.GlobalExceptionHandler;
import dev.runtime_lab.flowit.global.web.response.ApiListData;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static dev.runtime_lab.flowit.docs.support.DocumentedTypes.numberParameter;
import static dev.runtime_lab.flowit.docs.support.DocumentedTypes.stringParameter;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class WorkspaceActivityRecordsApiDocsTest {

	private final WorkspaceService workspaceService = mock(WorkspaceService.class);
	private final WorkspaceActivityService workspaceActivityService = mock(WorkspaceActivityService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new WorkspaceController(workspaceService, workspaceActivityService))
			.setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
			.setControllerAdvice(new ApiResponseBodyAdvice(), new GlobalExceptionHandler())
			.setValidator(validator)
			.apply(documentationConfiguration(restDocumentation))
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void activityRecords() throws Exception {
		ActivityRecordResponse record = new ActivityRecordResponse(
			7001L,
			1780920000L,
			ActivityRecordDomain.TASK,
			new ActivityActorResponse(3001L, 1001L, "Actor"),
			new ActivityTargetResponse(ActivityTargetType.TASK, 1001L, "Login UI"),
			ActivityRecordAction.STATUS_CHANGED,
			1,
			List.of(ActivityChangeElement.STATUS),
			List.of(new ActivityChangeResponse(ActivityChangeElement.STATUS, "TODO", "IN_PROGRESS"))
		);

		when(workspaceActivityService.activityRecords(any(CurrentUser.class), eq(2001L), any(ActivityRecordListQuery.class)))
			.thenReturn(ApiListData.of(List.of(record), 1L));
		SecurityContextHolder.getContext().setAuthentication(
			new JwtAuthenticationToken(jwt("1001", "actor@example.com", "Actor"), List.of())
		);

		mockMvc.perform(get("/v1/workspaces/{workspaceId}/activity-records", 2001L)
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
				.param("topic", "TASK")
				.param("rangeDays", "7")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("workspaces-activity-records",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(
					numberParameter("workspaceId").description("최근 활동을 조회할 워크스페이스 식별자입니다.")
				),
				requestHeaders(
					headerWithName(HttpHeaders.AUTHORIZATION).description("JWT access token입니다. `Bearer {token}` 형식으로 전달합니다."),
					headerWithName(HttpHeaders.ACCEPT).description("클라이언트가 기대하는 응답 미디어 타입입니다.").optional()
				),
				queryParameters(
					stringParameter("topic").description("활동 조회 주제입니다. 생략하면 `ALL`입니다. link:enum-reference.html#activity-record-topic[ActivityRecordTopic]을 참고합니다.").optional(),
					numberParameter("rangeDays").description("오늘부터 N일 전까지의 활동 범위입니다. 생략하면 5일입니다.").optional()
				),
				responseHeaders(
					headerWithName(HttpHeaders.CONTENT_TYPE).description("응답 본문 미디어 타입입니다.")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 처리 성공 여부입니다."),
					fieldWithPath("data").type(JsonFieldType.OBJECT).description("워크스페이스 최근 활동 목록 데이터입니다."),
					fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("최근 활동 목록입니다. `items[]` 1개가 기본 목록의 렌더링 단위입니다."),
					fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("활동 레코드 식별자입니다."),
					fieldWithPath("data.items[].occurredAt").type(JsonFieldType.NUMBER).description("활동 발생 시각입니다. Unix epoch seconds 기준입니다."),
					fieldWithPath("data.items[].domain").type(JsonFieldType.STRING).description("활동 도메인입니다. link:enum-reference.html#activity-record-domain[ActivityRecordDomain]을 참고합니다."),
					fieldWithPath("data.items[].actor").type(JsonFieldType.OBJECT).description("활동 수행자 정보입니다."),
					fieldWithPath("data.items[].actor.memberId").type(JsonFieldType.NUMBER).description("활동 수행자의 워크스페이스 멤버 식별자입니다. 시스템 이벤트에서는 `null`일 수 있습니다.").optional(),
					fieldWithPath("data.items[].actor.userId").type(JsonFieldType.NUMBER).description("활동 수행자의 사용자 식별자입니다. 시스템 이벤트에서는 `null`일 수 있습니다.").optional(),
					fieldWithPath("data.items[].actor.displayName").type(JsonFieldType.STRING).description("활동 발생 당시 수행자 표시 이름 스냅샷입니다."),
					fieldWithPath("data.items[].target").type(JsonFieldType.OBJECT).description("활동 대상 정보입니다."),
					fieldWithPath("data.items[].target.type").type(JsonFieldType.STRING).description("활동 대상 타입입니다. link:enum-reference.html#activity-target-type[ActivityTargetType]을 참고합니다."),
					fieldWithPath("data.items[].target.id").type(JsonFieldType.NUMBER).description("활동 대상 식별자입니다."),
					fieldWithPath("data.items[].target.displayName").type(JsonFieldType.STRING).description("활동 발생 당시 대상 표시 이름 스냅샷입니다."),
					fieldWithPath("data.items[].action").type(JsonFieldType.STRING).description("활동 액션입니다. 한 활동 레코드는 하나의 액션만 가집니다. link:enum-reference.html#activity-record-action[ActivityRecordAction]을 참고합니다."),
					fieldWithPath("data.items[].changeCount").type(JsonFieldType.NUMBER).description("해당 활동의 세부 변경 개수입니다. 최근 활동 목록에서는 요약 표시 용도로 사용할 수 있습니다."),
					fieldWithPath("data.items[].changedElements").type(JsonFieldType.ARRAY).description("해당 활동에서 바뀐 요소 목록입니다. 기본 목록 렌더링은 이 요약 필드를 우선 사용합니다."),
					fieldWithPath("data.items[].changes").type(JsonFieldType.ARRAY).description("단일 `action`에 딸린 상세 diff 목록입니다. 기본 활동 목록의 렌더링 단위가 아니며, 펼침/상세 UI에서 사용합니다."),
					fieldWithPath("data.items[].changes[].element").type(JsonFieldType.STRING).description("변경 요소입니다. link:enum-reference.html#activity-change-element[ActivityChangeElement]를 참고합니다."),
					fieldWithPath("data.items[].changes[].from").type(JsonFieldType.VARIES).description("변경 전 값입니다. 값 타입은 `element`에 따라 달라집니다.").optional(),
					fieldWithPath("data.items[].changes[].to").type(JsonFieldType.VARIES).description("변경 후 값입니다. 값 타입은 `element`에 따라 달라집니다.").optional(),
					fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER).description("반환된 활동 레코드 수입니다."),
					fieldWithPath("extensions").type(JsonFieldType.OBJECT).description("응답 보조 정보입니다.")
				)
			));
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
