package dev.runtime_lab.flowit.global.outbox.service;

import dev.runtime_lab.flowit.global.outbox.entity.OutboxEvent;
import dev.runtime_lab.flowit.global.outbox.entity.OutboxEventType;
import dev.runtime_lab.flowit.global.outbox.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventProcessor {

	private final OutboxEventRepository outboxEventRepository;
	private final Clock clock;
	private final Map<OutboxEventType, OutboxEventHandler> handlers;

	public OutboxEventProcessor(
		OutboxEventRepository outboxEventRepository,
		Clock clock,
		List<OutboxEventHandler> handlers
	) {
		this.outboxEventRepository = outboxEventRepository;
		this.clock = clock;
		this.handlers = handlersByType(handlers);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void process(Long outboxEventId) {
		OutboxEvent event = outboxEventRepository.findPendingByIdForUpdate(outboxEventId)
			.orElse(null);
		if (event == null) {
			return;
		}

		handler(event.getEventType()).handle(event);
		event.markProcessed(now());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(Long outboxEventId, RuntimeException exception) {
		outboxEventRepository.findPendingByIdForUpdate(outboxEventId)
			.ifPresent(event -> event.markFailed(now(), exception.getMessage()));
	}

	private OutboxEventHandler handler(OutboxEventType eventType) {
		OutboxEventHandler handler = handlers.get(eventType);
		if (handler == null) {
			throw new IllegalStateException("No outbox event handler for event type: " + eventType);
		}

		return handler;
	}

	private Map<OutboxEventType, OutboxEventHandler> handlersByType(List<OutboxEventHandler> handlers) {
		Map<OutboxEventType, OutboxEventHandler> handlersByType = new EnumMap<>(OutboxEventType.class);
		for (OutboxEventHandler handler : handlers) {
			handlersByType.put(handler.supports(), handler);
		}
		return handlersByType;
	}

	private Long now() {
		return Instant.now(clock).getEpochSecond();
	}
}
