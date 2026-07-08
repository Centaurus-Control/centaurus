package de.shadowsoft.centaurus.server.machine;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class MachineEventStreamService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineEventStreamService.class);
    private static final long STREAM_TIMEOUT_MS = 30L * 60L * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(new MachineEventStreamConnectedResponse(Instant.now())));
        } catch (IOException exception) {
            emitters.remove(emitter);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    @TransactionalEventListener
    public void handleMachineStatusChanged(MachineStatusChangedEvent event) {
        send("machine-status-changed", event);
    }

    private void send(String eventName, Object eventData) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(eventData));
            } catch (IOException | IllegalStateException exception) {
                emitters.remove(emitter);
                try {
                    emitter.completeWithError(exception);
                } catch (IllegalStateException completionException) {
                    LOGGER.debug("SSE emitter was already completed", completionException);
                }
            }
        }
    }

    private record MachineEventStreamConnectedResponse(Instant connectedAt) {
    }
}
