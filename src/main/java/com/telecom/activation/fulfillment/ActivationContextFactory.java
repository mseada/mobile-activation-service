package com.telecom.activation.fulfillment;

import com.telecom.activation.api.model.ProductOrder;
import com.telecom.activation.api.model.ProductOrderItem;
import com.telecom.activation.fulfillment.model.ActivationContext;
import com.telecom.activation.service.MobileActivationOrderValidator;
import org.springframework.stereotype.Component;

@Component
public class ActivationContextFactory {

    public ActivationContext create(ProductOrder order, ProductOrderItem item) {
        ActivationContext context = new ActivationContext();
        context.setOrderId(order.getId());
        context.setOrderItemId(item.getId());
        context.setCustomerId(MobileActivationOrderValidator.customerId(order.getRelatedParty())
                .orElseThrow(() -> new IllegalStateException("Customer relatedParty missing on order " + order.getId())));
        context.setMsisdn(MobileActivationOrderValidator.normalizeMsisdn(
                item.getProduct().characteristicValue(MobileActivationOrderValidator.MSISDN).orElseThrow()));
        context.setIccid(item.getProduct().characteristicValue(MobileActivationOrderValidator.ICCID).orElseThrow());
        context.setProductOfferingId(item.getProductOffering().getId());
        if (order.getBillingAccount() != null) {
            context.setBillingAccountId(order.getBillingAccount().getId());
        }
        return context;
    }
}
