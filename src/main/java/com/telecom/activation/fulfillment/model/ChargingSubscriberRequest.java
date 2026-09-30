package com.telecom.activation.fulfillment.model;

public record ChargingSubscriberRequest(
        String msisdn,
        String iccid,
        String productOfferingId,
        String billingSubscriptionId) {
}
