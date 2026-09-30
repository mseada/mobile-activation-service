package com.telecom.activation.service;

import com.telecom.activation.api.model.OrderItemActionType;
import com.telecom.activation.api.model.ProductOrderCreate;
import com.telecom.activation.api.model.ProductOrderItem;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MobileActivationOrderValidator {

    public static final String MSISDN = "MSISDN";
    public static final String ICCID = "ICCID";
    public static final String CUSTOMER_ROLE = "Customer";

    private static final Pattern MSISDN_PATTERN = Pattern.compile("^[1-9]\\d{7,14}$");
    private static final Pattern ICCID_PATTERN = Pattern.compile("^\\d{19,20}$");

    public void validate(ProductOrderCreate order) {
        boolean hasCustomer = order.getRelatedParty() != null && order.getRelatedParty().stream()
                .anyMatch(p -> CUSTOMER_ROLE.equalsIgnoreCase(p.getRole()) && p.getId() != null && !p.getId().isBlank());
        if (!hasCustomer) {
            throw new InvalidProductOrderException("relatedParty with role 'Customer' and an id is required");
        }

        Set<String> itemIds = new HashSet<>();
        Set<String> msisdns = new HashSet<>();
        for (ProductOrderItem item : order.getProductOrderItem()) {
            if (!itemIds.add(item.getId())) {
                throw new InvalidProductOrderException("Duplicate productOrderItem id: " + item.getId());
            }
            if (item.getAction() != OrderItemActionType.ADD) {
                throw new InvalidProductOrderException(
                        "productOrderItem " + item.getId() + ": only action 'add' is supported for mobile activation");
            }
            if (item.getProductOffering() == null || isBlank(item.getProductOffering().getId())) {
                throw new InvalidProductOrderException(
                        "productOrderItem " + item.getId() + ": productOffering.id (tariff plan) is required");
            }
            String msisdn = normalizeMsisdn(item.getProduct().characteristicValue(MSISDN).orElse(null));
            if (msisdn == null || !MSISDN_PATTERN.matcher(msisdn).matches()) {
                throw new InvalidProductOrderException(
                        "productOrderItem " + item.getId() + ": productCharacteristic 'MSISDN' must be an E.164 number");
            }
            if (!msisdns.add(msisdn)) {
                throw new InvalidProductOrderException("Duplicate MSISDN in order: " + msisdn);
            }
            String iccid = item.getProduct().characteristicValue(ICCID).orElse(null);
            if (iccid == null || !ICCID_PATTERN.matcher(iccid).matches()) {
                throw new InvalidProductOrderException(
                        "productOrderItem " + item.getId() + ": productCharacteristic 'ICCID' must be 19-20 digits");
            }
        }
    }

    public static String normalizeMsisdn(String msisdn) {
        if (msisdn == null) {
            return null;
        }
        String trimmed = msisdn.trim();
        return trimmed.startsWith("+") ? trimmed.substring(1) : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
