package com.telecom.activation.fulfillment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChargingSubscriberResponse(
        String subscriberId,
        String status,
        BigDecimal initialBalance) {
}
