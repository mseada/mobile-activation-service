package com.telecom.activation.fulfillment.model;

public record CrmAssetRequest(
        String orderId,
        String msisdn,
        String iccid,
        String productOfferingId,
        String billingSubscriptionId,
        String chargingSubscriberId) {
}
