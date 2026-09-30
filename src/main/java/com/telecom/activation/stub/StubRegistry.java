package com.telecom.activation.stub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * In-memory record of calls received by the stubs and events published to the Kafka stub.
 */
@Component
@ConditionalOnStubsEnabled
@RequiredArgsConstructor
public class StubRegistry {

    private final ObjectMapper objectMapper;
    private final List<StubInvocation> invocations = new CopyOnWriteArrayList<>();
    private final List<PublishedEvent> events = new CopyOnWriteArrayList<>();

    public void recordInvocation(String system, String operation, String msisdn, int responseStatus) {
        invocations.add(new StubInvocation(system, operation, msisdn, responseStatus, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    public void recordEvent(String key, String payload) throws JsonProcessingException {
        events.add(new PublishedEvent(key, objectMapper.readTree(payload), OffsetDateTime.now(ZoneOffset.UTC)));
    }

    public List<StubInvocation> invocations() {
        return List.copyOf(invocations);
    }

    public List<PublishedEvent> events() {
        return List.copyOf(events);
    }

    public void clear() {
        invocations.clear();
        events.clear();
    }
}
