package dev.runtime_lab.flowit.domain.notification.service.internal;

import dev.runtime_lab.flowit.domain.notification.event.NotificationAlertCreatedEvent;
import dev.runtime_lab.flowit.global.socket.WebSocketPublisher;
import dev.runtime_lab.flowit.global.socket.dto.WebSocketPayload;
import dev.runtime_lab.flowit.global.stereotype.InternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@InternalService
@RequiredArgsConstructor
@Slf4j
public class NotificationAlertSocketEventListener {

	private final NotificationAlertSocketDispatchLoader notificationAlertSocketDispatchLoader;
	private final WebSocketPublisher webSocketPublisher;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publishNotification(NotificationAlertCreatedEvent event) {
		try {
			notificationAlertSocketDispatchLoader.load(event.notificationAlertId())
				.ifPresent(dispatch -> publish(event.notificationAlertId(), dispatch));
		}
		catch (RuntimeException exception) {
			log.warn(
				"Failed to publish notification websocket event. notificationAlertId={}",
				event.notificationAlertId(),
				exception
			);
		}
	}

	private void publish(Long notificationAlertId, NotificationAlertSocketDispatch dispatch) {
		dispatch.recipientUserIds()
			.forEach(userId -> publish(notificationAlertId, userId, dispatch.payload()));
	}

	private void publish(Long notificationAlertId, Long userId, WebSocketPayload payload) {
		try {
			webSocketPublisher.publishUserNotification(userId, payload);
		}
		catch (RuntimeException exception) {
			log.warn(
				"Failed to publish notification websocket payload. notificationAlertId={} userId={}",
				notificationAlertId,
				userId,
				exception
			);
		}
	}
}
