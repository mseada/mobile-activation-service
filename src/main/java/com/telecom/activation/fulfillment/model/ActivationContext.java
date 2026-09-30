package com.telecom.activation.fulfillment.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ActivationContext {

    private String orderId;
    private String orderItemId;
    private String customerId;
    private String msisdn;
    private String iccid;
    private String productOfferingId;
    private String billingAccountId;
    private String billingSubscriptionId;
    private String chargingSubscriberId;
    private String crmAssetId;
}
