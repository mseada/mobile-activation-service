package com.telecom.activation.fulfillment.model;

public record BillingSubscriptionRequest(
        String orderId,
        String customerId,
        String billingAccountId,
        String msisdn,
        String productOfferingId) {
}
