package com.telecom.activation.stub;

import java.time.OffsetDateTime;

public record StubInvocation(String system, String operation, String msisdn, int responseStatus, OffsetDateTime timestamp) {
}
