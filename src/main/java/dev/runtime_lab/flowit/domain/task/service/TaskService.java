package dev.runtime_lab.flowit.domain.task.service;

import dev.runtime_lab.flowit.domain.activity.service.internal.WorkspaceActivityRecorder;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskCommentResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskDetailResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskHistoryChangeResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskHistoryResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskListQuery;
import dev.runtime_lab.flowit.domain.task.dto.TaskProgressUpdateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskSummaryResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskUpdateRequest;
import dev.runtime_lab.flowit.domain.task.entity.Task;
import dev.runtime_lab.flowit.domain.task.entity.TaskChangeHistory;
import dev.runtime_lab.flowit.domain.task.entity.TaskHistoryAction;
import dev.runtime_lab.flowit.domain.task.entity.TaskHistoryElement;
import dev.runtime_lab.flowit.domain.task.entity.TaskPriority;
import dev.runtime_lab.flowit.domain.task.entity.TaskStatus;
import dev.runtime_lab.flowit.domain.task.entity.TaskTag;
import dev.runtime_lab.flowit.domain.task.exception.InvalidTaskRequestException;
import dev.runtime_lab.flowit.domain.task.exception.TaskNotFoundException;
import dev.runtime_lab.flowit.domain.task.repository.TaskChangeHistoryRepository;
import dev.runtime_lab.flowit.domain.task.repository.TaskCommentRepository;
import dev.runtime_lab.flowit.domain.task.repository.TaskRepository;
import dev.runtime_lab.flowit.domain.task.repository.TaskTagRepository;
import dev.runtime_lab.flowit.domain.user.entity.User;
import dev.runtime_lab.flowit.domain.workspace.entity.Workspace;
import dev.runtime_lab.flowit.domain.workspace.entity.WorkspaceMember;
import dev.runtime_lab.flowit.domain.workspace.service.internal.contract.WorkspaceAccessContext;
import dev.runtime_lab.flowit.domain.workspace.service.internal.WorkspaceAccessService;
import dev.runtime_lab.flowit.global.security.authentication.CurrentUser;
import dev.runtime_lab.flowit.global.web.response.ApiListData;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.DEFAULT_HISTORY_PAGE_SIZE;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.DEFAULT_COMMENT_PAGE_SIZE;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.MAX_HISTORY_PAGE_SIZE;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.TAG_MAX_COUNT;
import static dev.runtime_lab.flowit.domain.task.validation.TaskConstraints.TAG_MAX_LENGTH;
@Service
@RequiredArgsConstructor
public class TaskService {

	private final WorkspaceAccessService workspaceAccessService;
	private final TaskRepository taskRepository;
	private final TaskTagRepository taskTagRepository;
	private final TaskChangeHistoryRepository taskChangeHistoryRepository;
	private final TaskCommentRepository taskCommentRepository;
	private final WorkspaceActivityRecorder workspaceActivityRecorder;
	private final JsonMapper jsonMapper;
	private final Clock clock;

	@Transactional
	public TaskCreateResponse create(CurrentUser currentUser, Long workspaceId, TaskCreateRequest request) {
		WorkspaceAccessContext access = workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);
		User requester = access.requester();
		Workspace workspace = access.workspace();
		WorkspaceMember actorMember = access.membership();
		WorkspaceMember assignee = findAssignee(workspaceId, request.assigneeMemberId());
		List<TagValue> tags = normalizeTags(request.tags());
		long now = now();

		Task task = taskRepository.save(Task.builder()
			.workspace(workspace)
			.title(request.title().trim())
			.descriptionMarkdown(request.descriptionMarkdown())
			.status(request.status() == null ? TaskStatus.TO_DO : request.status())
			.priority(request.priority())
			.assignee(assignee)
			.startDate(request.startDate())
			.dueDate(request.dueDate())
			.progress(0)
			.createdBy(requester)
			.createdAt(now)
			.updatedAt(now)
			.build());

		insertTags(task, tags, now);

		List<TaskHistoryChangeResponse> changes = new ArrayList<>();
		changes.add(change(TaskHistoryElement.TITLE, null, task.getTitle()));
		changes.add(change(TaskHistoryElement.STATUS, null, task.getStatus().name()));
		addChangeIfChanged(changes, TaskHistoryElement.ASSIGNEE, null, memberValue(task.getAssignee()));
		changes.add(change(TaskHistoryElement.PRIORITY, null, task.getPriority().name()));
		changes.add(change(TaskHistoryElement.TAGS, List.of(), tagNamesFromValues(tags)));

		recordHistory(
			workspace,
			task,
			actorMember,
			requester,
			TaskHistoryAction.CREATED,
			changes,
			now
		);

		return TaskCreateResponse.from(task);
	}

	@Transactional
	public void update(CurrentUser currentUser, Long workspaceId, Long taskId, TaskUpdateRequest request) {
		WorkspaceAccessContext access = workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);
		User requester = access.requester();
		WorkspaceMember actorMember = access.membership();
		Task task = findTaskForUpdate(workspaceId, taskId);
		WorkspaceMember assignee = findAssignee(workspaceId, request.assigneeMemberId());

		List<String> oldTags = tagNames(taskTagRepository.findByTaskId(task.getId()));
		List<TagValue> newTags = normalizeTags(request.tags());
		List<String> newTagNames = tagNamesFromValues(newTags);
		List<TaskHistoryChangeResponse> changes = updateChanges(task, request, assignee, oldTags, newTagNames);

		if (changes.isEmpty()) {
			return;
		}

		long now = now();
		task.update(
			request.title().trim(),
			request.descriptionMarkdown(),
			request.status(),
			assignee,
			request.priority(),
			request.startDate(),
			request.dueDate(),
			now
		);

		if (!oldTags.equals(newTagNames)) {
			taskTagRepository.deleteByTaskId(task.getId());
			insertTags(task, newTags, now);
		}

		recordHistory(
			task.getWorkspace(),
			task,
			actorMember,
			requester,
			historyAction(changes),
			changes,
			now
		);
	}

	@Transactional(readOnly = true)
	public ApiListData<TaskSummaryResponse> tasks(CurrentUser currentUser, Long workspaceId, TaskListQuery query) {
		workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);

		String normalizedTag = normalizeOptionalTag(query.tag());
		List<Task> tasks = taskRepository.findActiveByWorkspaceId(workspaceId, query, normalizedTag);
		Map<Long, List<String>> tagsByTaskId = tagsByTaskId(tasks);

		List<TaskSummaryResponse> responses = tasks.stream()
			.map(task -> TaskSummaryResponse.from(task, tagsByTaskId.getOrDefault(task.getId(), List.of())))
			.toList();

		long totalCount = taskRepository.countActiveByWorkspaceId(workspaceId, query, normalizedTag);

		return ApiListData.of(responses, totalCount);
	}

	@Transactional(readOnly = true)
	public TaskDetailResponse get(CurrentUser currentUser, Long workspaceId, Long taskId) {
		WorkspaceAccessContext access = workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);

		Task task = taskRepository.findActiveByWorkspaceIdAndTaskId(workspaceId, taskId)
			.orElseThrow(TaskNotFoundException::new);

		List<String> tags = tagNames(taskTagRepository.findByTaskId(task.getId()));
		ApiListData<TaskCommentResponse> commentPage = initialCommentPage(
			workspaceId,
			taskId,
			access.requester().getId()
		);

		return TaskDetailResponse.from(task, tags, commentPage);
	}

	@Transactional
	public void updateProgress(
		CurrentUser currentUser,
		Long workspaceId,
		Long taskId,
		TaskProgressUpdateRequest request
	) {
		WorkspaceAccessContext access = workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);
		User requester = access.requester();
		WorkspaceMember actorMember = access.membership();
		Task task = findTaskForUpdate(workspaceId, taskId);

		if (Objects.equals(task.getProgress(), request.progress())) {
			return;
		}

		Integer oldProgress = task.getProgress();

		long now = now();
		task.updateProgress(request.progress(), now);

		recordHistory(
			task.getWorkspace(),
			task,
			actorMember,
			requester,
			TaskHistoryAction.PROGRESS_CHANGED,
			List.of(change(TaskHistoryElement.PROGRESS, oldProgress, request.progress())),
			now
		);
	}

	@Transactional(readOnly = true)
	public ApiListData<TaskHistoryResponse> taskHistories(
		CurrentUser currentUser,
		Long workspaceId,
		Long taskId,
		Integer page,
		Integer size
	) {
		workspaceAccessService.resolveMemberAccess(currentUser, workspaceId);
		if (taskRepository.findActiveByWorkspaceIdAndTaskId(workspaceId, taskId).isEmpty()) {
			throw new TaskNotFoundException();
		}

		int pageValue = page(page);
		int sizeValue = size(size);

		List<TaskHistoryResponse> responses = taskChangeHistoryRepository
			.findByWorkspaceIdAndTaskId(workspaceId, taskId, pageValue, sizeValue)
			.stream()
			.map(this::historyResponse)
			.toList();

		long totalCount = taskChangeHistoryRepository.countByWorkspaceIdAndTaskId(workspaceId, taskId);

		return ApiListData.of(responses, totalCount);
	}

	private WorkspaceMember findAssignee(Long workspaceId, Long assigneeMemberId) {
		if (assigneeMemberId == null) {
			return null;
		}

		return workspaceAccessService.findActiveMember(workspaceId, assigneeMemberId)
			.orElseThrow(() -> new InvalidTaskRequestException("작업은 활성 멤버에게만 할당될 수 있습니다."));
	}

	private Task findTaskForUpdate(Long workspaceId, Long taskId) {
		return taskRepository.findActiveByWorkspaceIdAndTaskIdForUpdate(workspaceId, taskId)
			.orElseThrow(TaskNotFoundException::new);
	}

	private ApiListData<TaskCommentResponse> initialCommentPage(Long workspaceId, Long taskId, Long requesterUserId) {
		List<TaskCommentResponse> comments = taskCommentRepository
			.findActiveByWorkspaceIdAndTaskId(workspaceId, taskId, 0, DEFAULT_COMMENT_PAGE_SIZE)
			.stream()
			.map(comment -> TaskCommentResponse.from(comment, requesterUserId))
			.toList();
		long totalCount = taskCommentRepository.countActiveByWorkspaceIdAndTaskId(workspaceId, taskId);

		return ApiListData.of(comments, totalCount);
	}

	private List<TaskHistoryChangeResponse> updateChanges(
		Task task,
		TaskUpdateRequest request,
		WorkspaceMember assignee,
		List<String> oldTags,
		List<String> newTags
	) {
		List<TaskHistoryChangeResponse> changes = new ArrayList<>();
		addChangeIfChanged(changes, TaskHistoryElement.TITLE, task.getTitle(), request.title().trim());

		if (!Objects.equals(task.getDescriptionMarkdown(), request.descriptionMarkdown())) {
			changes.add(change(TaskHistoryElement.DESCRIPTION, null, null));
		}

		addChangeIfChanged(changes, TaskHistoryElement.STATUS, task.getStatus(), request.status());
		addChangeIfChanged(changes, TaskHistoryElement.ASSIGNEE, memberValue(task.getAssignee()), memberValue(assignee));
		addChangeIfChanged(changes, TaskHistoryElement.PRIORITY, task.getPriority(), request.priority());
		addChangeIfChanged(changes, TaskHistoryElement.START_DATE, task.getStartDate(), request.startDate());
		addChangeIfChanged(changes, TaskHistoryElement.DUE_DATE, task.getDueDate(), request.dueDate());
		addChangeIfChanged(changes, TaskHistoryElement.TAGS, oldTags, newTags);

		return changes;
	}

	private TaskHistoryAction historyAction(List<TaskHistoryChangeResponse> changes) {
		boolean statusChanged = changes.stream()
			.anyMatch(change -> change.element() == TaskHistoryElement.STATUS);

		if (statusChanged) {
			return TaskHistoryAction.STATUS_CHANGED;
		}

		return TaskHistoryAction.MODIFIED;
	}

	private void addChangeIfChanged(
		List<TaskHistoryChangeResponse> changes,
		TaskHistoryElement element,
		Object from,
		Object to
	) {
		if (!Objects.equals(from, to)) {
			changes.add(change(element, from, to));
		}
	}

	private TaskHistoryChangeResponse change(TaskHistoryElement element, Object from, Object to) {
		return new TaskHistoryChangeResponse(element, historyValue(from), historyValue(to));
	}

	private Object historyValue(Object value) {
		if (value instanceof TaskStatus status) {
			return status.name();
		}

		if (value instanceof TaskPriority priority) {
			return priority.name();
		}

		return value;
	}

	private Map<String, Object> memberValue(WorkspaceMember workspaceMember) {
		if (workspaceMember == null) {
			return null;
		}

		return Map.of(
			"memberId", workspaceMember.getId(),
			"userId", workspaceMember.getUser().getId(),
			"displayName", workspaceMember.getUser().getName()
		);
	}

	private void insertTags(Task task, List<TagValue> tags, long now) {
		for (int index = 0; index < tags.size(); index++) {
			TagValue tag = tags.get(index);
			taskTagRepository.save(TaskTag.builder()
				.task(task)
				.name(tag.name())
				.normalizedName(tag.normalizedName())
				.sortOrder(index)
				.createdAt(now)
				.build());
		}
	}

	private List<TagValue> normalizeTags(List<String> source) {
		if (source == null || source.isEmpty()) {
			return List.of();
		}

		LinkedHashMap<String, TagValue> tagsByNormalizedName = new LinkedHashMap<>();
		for (String rawTag : source) {
			String name = normalizeDisplayTag(rawTag);
			String normalizedName = normalizeTagName(name);
			tagsByNormalizedName.putIfAbsent(normalizedName, new TagValue(name, normalizedName));
		}

		if (tagsByNormalizedName.size() > TAG_MAX_COUNT) {
			throw new InvalidTaskRequestException("작업의 태그는 10개를 넘을 수 없습니다.");
		}

		return List.copyOf(tagsByNormalizedName.values());
	}

	private String normalizeDisplayTag(String rawTag) {
		if (rawTag == null) {
			throw new InvalidTaskRequestException("작업의 태그는 null일 수 없습니다.");
		}

		String normalized = Normalizer.normalize(rawTag.trim(), Normalizer.Form.NFC);

		if (normalized.isBlank()) {
			throw new InvalidTaskRequestException("작업의 태그는 빈 값일 수 없습니다.");
		}

		if (normalized.length() > TAG_MAX_LENGTH) {
			throw new InvalidTaskRequestException("작업의 태그는 30자를 넘을 수 없습니다.");
		}

		return normalized;
	}

	private String normalizeTagName(String tag) {
		return tag.toLowerCase(Locale.ROOT);
	}

	private String normalizeOptionalTag(String tag) {
		if (tag == null || tag.isBlank()) {
			return null;
		}

		return normalizeTagName(normalizeDisplayTag(tag));
	}

	private List<String> tagNames(List<TaskTag> taskTags) {
		return taskTags.stream()
			.map(TaskTag::getName)
			.toList();
	}

	private List<String> tagNamesFromValues(List<TagValue> tags) {
		return tags.stream()
			.map(TagValue::name)
			.toList();
	}

	private Map<Long, List<String>> tagsByTaskId(List<Task> tasks) {
		List<Long> taskIds = tasks.stream()
			.map(Task::getId)
			.toList();

		return taskTagRepository.findByTaskIds(taskIds)
			.stream()
			.collect(Collectors.groupingBy(
				taskTag -> taskTag.getTask().getId(),
				LinkedHashMap::new,
				Collectors.mapping(TaskTag::getName, Collectors.toList())
			));
	}

	private void recordHistory(
		Workspace workspace,
		Task task,
		WorkspaceMember actorMember,
		User actorUser,
		TaskHistoryAction action,
		List<TaskHistoryChangeResponse> changes,
		long changedAt
	) {
		TaskChangeHistory history = TaskChangeHistory.builder()
			.workspace(workspace)
			.task(task)
			.taskTitleSnapshot(task.getTitle())
			.actorWorkspaceMember(actorMember)
			.actorUser(actorUser)
			.actorDisplayNameSnapshot(actorUser.getName())
			.action(action)
			.changesJson(writeChanges(changes))
			.changedAt(changedAt)
			.build();

		taskChangeHistoryRepository.save(history);
		workspaceActivityRecorder.recordTask(history, changes);
	}

	private String writeChanges(List<TaskHistoryChangeResponse> changes) {
		try {
			return jsonMapper.writeValueAsString(changes);
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("Failed to serialize task history changes.", exception);
		}
	}

	private List<TaskHistoryChangeResponse> readChanges(String changesJson) {
		try {
			return jsonMapper.readValue(changesJson, new TypeReference<>() {
			});
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("Failed to deserialize task history changes.", exception);
		}
	}

	private TaskHistoryResponse historyResponse(TaskChangeHistory history) {
		return TaskHistoryResponse.from(history, readChanges(history.getChangesJson()));
	}

	private long now() {
		return Instant.now(clock).getEpochSecond();
	}

	private int page(Integer page) {
		if (page == null || page < 0) {
			return 0;
		}

		return page;
	}

	private int size(Integer size) {
		if (size == null) {
			return DEFAULT_HISTORY_PAGE_SIZE;
		}
		if (size < 1) {
			return 1;
		}
		if (size > MAX_HISTORY_PAGE_SIZE) {
			return MAX_HISTORY_PAGE_SIZE;
		}

		return size;
	}

	private record TagValue(
		String name,
		String normalizedName
	) {
	}
}
