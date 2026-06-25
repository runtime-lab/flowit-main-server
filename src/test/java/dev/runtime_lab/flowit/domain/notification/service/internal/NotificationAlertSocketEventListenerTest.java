package dev.runtime_lab.flowit.domain.notification.service.internal;

import dev.runtime_lab.flowit.domain.notification.dto.NotificationAlertResponse;
import dev.runtime_lab.flowit.domain.notification.event.NotificationAlertCreatedEvent;
import dev.runtime_lab.flowit.global.socket.WebSocketPublisher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationAlertSocketEventListenerTest {

	private final NotificationAlertSocketDispatchLoader notificationAlertSocketDispatchLoader =
		mock(NotificationAlertSocketDispatchLoader.class);
	private final WebSocketPublisher webSocketPublisher = mock(WebSocketPublisher.class);
	private final NotificationAlertSocketEventListener listener = new NotificationAlertSocketEventListener(
		notificationAlertSocketDispatchLoader,
		webSocketPublisher
	);

	@Test
	void publishesCreatedNotificationToRecipientUserQueues() {
		NotificationAlertResponse response = mock(NotificationAlertResponse.class);

		when(notificationAlertSocketDispatchLoader.load(1L))
			.thenReturn(Optional.of(new NotificationAlertSocketDispatch(response, List.of(34L, 35L))));

		listener.publishNotification(new NotificationAlertCreatedEvent(1L, 1782013300L));

		verify(webSocketPublisher).publishUserNotification(34L, response);
		verify(webSocketPublisher).publishUserNotification(35L, response);
	}

	@Test
	void doesNotPublishWhenDispatchPayloadDoesNotExist() {
		when(notificationAlertSocketDispatchLoader.load(1L)).thenReturn(Optional.empty());

		listener.publishNotification(new NotificationAlertCreatedEvent(1L, 1782013300L));

		verifyNoInteractions(webSocketPublisher);
	}

	@Test
	void doesNotPropagateWebSocketPublishFailureAndContinuesRecipients() {
		NotificationAlertResponse response = mock(NotificationAlertResponse.class);

		when(notificationAlertSocketDispatchLoader.load(1L))
			.thenReturn(Optional.of(new NotificationAlertSocketDispatch(response, List.of(34L, 35L))));
		doThrow(new IllegalStateException("broker down"))
			.when(webSocketPublisher)
			.publishUserNotification(eq(34L), same(response));

		assertDoesNotThrow(() -> listener.publishNotification(new NotificationAlertCreatedEvent(1L, 1782013300L)));

		verify(webSocketPublisher).publishUserNotification(34L, response);
		verify(webSocketPublisher).publishUserNotification(35L, response);
	}

	@Test
	void doesNotPropagateDispatchLoadFailure() {
		when(notificationAlertSocketDispatchLoader.load(1L))
			.thenThrow(new IllegalStateException("dispatch load failed"));

		assertDoesNotThrow(() -> listener.publishNotification(new NotificationAlertCreatedEvent(1L, 1782013300L)));

		verifyNoInteractions(webSocketPublisher);
	}
}
