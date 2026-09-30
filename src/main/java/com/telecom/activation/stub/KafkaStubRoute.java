package com.telecom.activation.stub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.stereotype.Component;

/**
 * Stands in for the Kafka broker: events sent to {@value #ENDPOINT} are kept in memory.
 */
@Component
@ConditionalOnStubsEnabled
@RequiredArgsConstructor
public class KafkaStubRoute extends RouteBuilder {

    public static final String ENDPOINT = "direct:kafkaStub";
    static final String SYSTEM = "kafka";

    private final StubProperties properties;
    private final StubRegistry registry;
    private final ObjectMapper objectMapper;

    @Override
    public void configure() {
        from(ENDPOINT)
                .routeId("kafka-stub")
                .process(exchange -> {
                    String key = exchange.getMessage().getHeader(KafkaConstants.KEY, String.class);
                    String payload = exchange.getMessage().getBody(String.class);
                    JsonNode msisdn = objectMapper.readTree(payload).get("msisdn");
                    String eventMsisdn = msisdn == null ? key : msisdn.asText();
                    if (properties.shouldFail(SYSTEM, eventMsisdn)) {
                        registry.recordInvocation(SYSTEM, "publish", eventMsisdn, 503);
                        throw new IllegalStateException("Kafka broker unavailable (stub)");
                    }
                    registry.recordInvocation(SYSTEM, "publish", eventMsisdn, 200);
                    registry.recordEvent(key, payload);
                })
                .log("Kafka stub received event for key ${header.kafka.KEY}");
    }
}
