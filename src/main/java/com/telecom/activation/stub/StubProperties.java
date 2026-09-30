package com.telecom.activation.stub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the embedded external-system stubs. {@code failingMsisdns} maps a system name
 * (billing, charging, crm, kafka) to MSISDNs for which that stub simulates an outage (HTTP 503).
 */
@Data
@ConfigurationProperties(prefix = "stubs")
public class StubProperties {

    private boolean enabled = true;

    private Map<String, List<String>> failingMsisdns = new HashMap<>();

    public boolean shouldFail(String system, String msisdn) {
        return failingMsisdns.getOrDefault(system, List.of()).contains(msisdn);
    }
}
