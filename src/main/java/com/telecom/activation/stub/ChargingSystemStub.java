package com.telecom.activation.stub;

import com.telecom.activation.api.model.ErrorResponse;
import com.telecom.activation.fulfillment.model.ChargingSubscriberRequest;
import com.telecom.activation.fulfillment.model.ChargingSubscriberResponse;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnStubsEnabled
@RequiredArgsConstructor
@RequestMapping("/stubs/charging/api/v1")
public class ChargingSystemStub {

    static final String SYSTEM = "charging";

    private final StubProperties properties;
    private final StubRegistry registry;

    @PostMapping("/subscribers")
    public ResponseEntity<?> createSubscriber(@RequestBody ChargingSubscriberRequest request) {
        if (properties.shouldFail(SYSTEM, request.msisdn())) {
            registry.recordInvocation(SYSTEM, "createSubscriber", request.msisdn(), 503);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse.of(503, "Service Unavailable", "Charging system unavailable (stub)"));
        }
        registry.recordInvocation(SYSTEM, "createSubscriber", request.msisdn(), 201);
        String subscriberId = "OCS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ChargingSubscriberResponse(subscriberId, "ACTIVE", BigDecimal.ZERO));
    }
}
