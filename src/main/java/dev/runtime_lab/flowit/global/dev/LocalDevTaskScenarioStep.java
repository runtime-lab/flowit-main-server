package dev.runtime_lab.flowit.global.dev;

import dev.runtime_lab.flowit.domain.task.dto.TaskCreateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskCreateResponse;
import dev.runtime_lab.flowit.domain.task.dto.TaskProgressUpdateRequest;
import dev.runtime_lab.flowit.domain.task.dto.TaskUpdateRequest;
import dev.runtime_lab.flowit.domain.task.entity.Task;
import dev.runtime_lab.flowit.domain.task.repository.TaskRepository;
import dev.runtime_lab.flowit.domain.task.service.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDevTaskScenarioStep {

	private static final long DAY_SECONDS = 24L * 60L * 60L;

	private final TaskService taskService;
	private final TaskRepository taskRepository;
	private final Clock clock;
	private final LocalDevSampleRecordRegistry sampleRecordRegistry;

	public void reconcile(LocalDevScenarioContext context) {
		long now = Instant.now(clock).getEpochSecond();
		for (LocalDevScenarioSamples.SampleTask sampleTask : LocalDevScenarioSamples.TASKS) {
			Long taskId = reconcileTask(context, sampleTask, now);
			context.putTaskId(sampleTask.recordKey(), taskId);
		}
	}

	private Long reconcileTask(
		LocalDevScenarioContext context,
		LocalDevScenarioSamples.SampleTask sampleTask,
		long now
	) {
		Long taskId = findTrackedTask(context, sampleTask)
			.or(() -> taskRepository.findActiveByWorkspaceIdAndTitle(context.workspace().getId(), sampleTask.title()))
			.map(task -> reconcileExistingTask(context, sampleTask, task, now))
			.orElseGet(() -> createTask(context, sampleTask, now));

		sampleRecordRegistry.upsert(sampleTask.recordKey(), LocalDevSampleRecordType.TASK, taskId);
		return taskId;
	}

	private Optional<Task> findTrackedTask(
		LocalDevScenarioContext context,
		LocalDevScenarioSamples.SampleTask sampleTask
	) {
		return sampleRecordRegistry.findEntityId(sampleTask.recordKey(), LocalDevSampleRecordType.TASK)
			.flatMap(taskId -> taskRepository.findActiveByWorkspaceIdAndTaskId(context.workspace().getId(), taskId));
	}

	private Long reconcileExistingTask(
		LocalDevScenarioContext context,
		LocalDevScenarioSamples.SampleTask sampleTask,
		Task task,
		long now
	) {
		taskService.update(
			context.currentUser(),
			context.workspace().getId(),
			task.getId(),
			new TaskUpdateRequest(
				sampleTask.title(),
				sampleTask.descriptionMarkdown(),
				sampleTask.status(),
				context.ownerMembership().getId(),
				sampleTask.priority(),
				epochDay(now, sampleTask.startOffsetDays()),
				epochDay(now, sampleTask.dueOffsetDays()),
				sampleTask.tags()
			)
		);

		if (!Objects.equals(task.getProgress(), sampleTask.progress())) {
			taskService.updateProgress(
				context.currentUser(),
				context.workspace().getId(),
				task.getId(),
				new TaskProgressUpdateRequest(sampleTask.progress())
			);
		}

		return task.getId();
	}

	private Long createTask(
		LocalDevScenarioContext context,
		LocalDevScenarioSamples.SampleTask sampleTask,
		long now
	) {
		TaskCreateResponse response = taskService.create(
			context.currentUser(),
			context.workspace().getId(),
			new TaskCreateRequest(
				sampleTask.title(),
				sampleTask.descriptionMarkdown(),
				sampleTask.status(),
				context.ownerMembership().getId(),
				sampleTask.priority(),
				epochDay(now, sampleTask.startOffsetDays()),
				epochDay(now, sampleTask.dueOffsetDays()),
				sampleTask.progress(),
				sampleTask.tags()
			)
		);

		return response.id();
	}

	private Long epochDay(long now, Long offsetDays) {
		if (offsetDays == null) {
			return null;
		}

		return now + (offsetDays * DAY_SECONDS);
	}
}
