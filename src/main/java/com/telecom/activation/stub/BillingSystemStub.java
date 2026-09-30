package com.telecom.activation.stub;

import com.telecom.activation.api.model.ErrorResponse;
import com.telecom.activation.fulfillment.model.BillingSubscriptionRequest;
import com.telecom.activation.fulfillment.model.BillingSubscriptionResponse;
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
@RequestMapping("/stubs/billing/api/v1")
public class BillingSystemStub {

    static final String SYSTEM = "billing";

    private final StubProperties properties;
    private final StubRegistry registry;

    @PostMapping("/subscriptions")
    public ResponseEntity<?> createSubscription(@RequestBody BillingSubscriptionRequest request) {
        if (properties.shouldFail(SYSTEM, request.msisdn())) {
            registry.recordInvocation(SYSTEM, "createSubscription", request.msisdn(), 503);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse.of(503, "Service Unavailable", "Billing system unavailable (stub)"));
        }
        registry.recordInvocation(SYSTEM, "createSubscription", request.msisdn(), 201);
        String billingAccountId = request.billingAccountId() != null
                ? request.billingAccountId()
                : "BA-" + shortId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BillingSubscriptionResponse("SUB-" + shortId(), billingAccountId, "ACTIVE"));
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
