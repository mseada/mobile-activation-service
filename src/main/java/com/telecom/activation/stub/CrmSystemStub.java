package com.telecom.activation.stub;

import com.telecom.activation.api.model.ErrorResponse;
import com.telecom.activation.fulfillment.model.CrmAssetRequest;
import com.telecom.activation.fulfillment.model.CrmAssetResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnStubsEnabled
@RequiredArgsConstructor
@RequestMapping("/stubs/crm/api/v1")
public class CrmSystemStub {

    static final String SYSTEM = "crm";

    private final StubProperties properties;
    private final StubRegistry registry;

    @PostMapping("/customers/{customerId}/assets")
    public ResponseEntity<?> createAsset(@PathVariable String customerId, @RequestBody CrmAssetRequest request) {
        if (properties.shouldFail(SYSTEM, request.msisdn())) {
            registry.recordInvocation(SYSTEM, "createAsset", request.msisdn(), 503);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse.of(503, "Service Unavailable", "CRM system unavailable (stub)"));
        }
        registry.recordInvocation(SYSTEM, "createAsset", request.msisdn(), 201);
        String assetId = "AST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return ResponseEntity.status(HttpStatus.CREATED).body(new CrmAssetResponse(assetId, "ACTIVE"));
    }
}
