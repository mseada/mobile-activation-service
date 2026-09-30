package com.telecom.activation.stub;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

public record PublishedEvent(String key, JsonNode payload, OffsetDateTime timestamp) {
}
