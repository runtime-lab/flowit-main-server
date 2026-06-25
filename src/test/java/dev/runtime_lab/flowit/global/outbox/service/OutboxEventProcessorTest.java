package dev.runtime_lab.flowit.global.outbox.service;

import dev.runtime_lab.flowit.global.outbox.entity.OutboxEvent;
import dev.runtime_lab.flowit.global.outbox.entity.OutboxEventStatus;
import dev.runtime_lab.flowit.global.outbox.entity.OutboxEventType;
import dev.runtime_lab.flowit.global.outbox.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventProcessorTest {

	private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
	private final Clock clock = Clock.fixed(Instant.ofEpochSecond(1782013400L), ZoneOffset.UTC);
	private final OutboxEventHandler handler = mock(OutboxEventHandler.class);

	@Test
	void processesOutboxEventInNewTransactionAfterCommitCallback() throws NoSuchMethodException {
		Transactional transactional = OutboxEventProcessor.class
			.getMethod("process", Long.class)
			.getAnnotation(Transactional.class);

		assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
	}

	@Test
	void processesPendingOutboxEvent() {
		OutboxEvent event = outboxEvent();

		when(handler.supports()).thenReturn(OutboxEventType.WORKSPACE_ACTIVITY_NOTIFICATION_REQUESTED);
		OutboxEventProcessor outboxEventProcessor =
			new OutboxEventProcessor(outboxEventRepository, clock, List.of(handler));
		when(outboxEventRepository.findPendingByIdForUpdate(1L)).thenReturn(Optional.of(event));

		outboxEventProcessor.process(1L);

		verify(handler).handle(event);
		assertEquals(OutboxEventStatus.PROCESSED, event.getStatus());
		assertEquals(1782013400L, event.getProcessedAt());
	}

	@Test
	void marksPendingOutboxEventAsFailed() {
		OutboxEvent event = outboxEvent();

		when(handler.supports()).thenReturn(OutboxEventType.WORKSPACE_ACTIVITY_NOTIFICATION_REQUESTED);
		OutboxEventProcessor outboxEventProcessor =
			new OutboxEventProcessor(outboxEventRepository, clock, List.of(handler));
		when(outboxEventRepository.findPendingByIdForUpdate(1L)).thenReturn(Optional.of(event));

		outboxEventProcessor.markFailed(1L, new IllegalStateException("failed"));

		assertEquals(OutboxEventStatus.FAILED, event.getStatus());
		assertEquals(1782013400L, event.getFailedAt());
		assertEquals("failed", event.getFailureMessage());
	}

	private OutboxEvent outboxEvent() {
		return OutboxEvent.builder()
			.eventType(OutboxEventType.WORKSPACE_ACTIVITY_NOTIFICATION_REQUESTED)
			.payloadJson("{}")
			.status(OutboxEventStatus.PENDING)
			.createdAt(1782013300L)
			.updatedAt(1782013300L)
			.build();
	}
}
