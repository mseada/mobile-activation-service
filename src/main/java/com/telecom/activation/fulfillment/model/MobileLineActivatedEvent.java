package com.telecom.activation.fulfillment.model;

import java.time.OffsetDateTime;

public record MobileLineActivatedEvent(
        String eventId,
        String eventType,
        OffsetDateTime eventTime,
        String orderId,
        String orderItemId,
        String customerId,
        String msisdn,
        String iccid,
        String productOfferingId,
        String billingAccountId,
        String billingSubscriptionId,
        String chargingSubscriberId,
        String crmAssetId) {

    public static final String EVENT_TYPE = "MobileLineActivatedEvent";
}
