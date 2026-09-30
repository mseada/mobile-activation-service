package com.telecom.activation.service;

import com.telecom.activation.api.model.OrderItemActionType;
import com.telecom.activation.api.model.ProductOrderCreate;
import com.telecom.activation.api.model.ProductOrderItem;
import com.telecom.activation.api.model.RelatedParty;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    public void validate(ProductOrderCreate order) {
        if (order.getRelatedParty() != null && order.getRelatedParty().contains(null)) {
            throw new InvalidProductOrderException("relatedParty entries must not be null");
        }
        String customerId = customerId(order.getRelatedParty())
                .orElseThrow(() -> new InvalidProductOrderException(
                        "relatedParty with role 'Customer' and an id is required"));
        if (!CUSTOMER_ID_PATTERN.matcher(customerId).matches()) {
            throw new InvalidProductOrderException(
                    "Customer id must be 1-64 characters of letters, digits, '.', '_' or '-'");
        }

        Set<String> itemIds = new HashSet<>();
        Set<String> msisdns = new HashSet<>();
        for (ProductOrderItem item : order.getProductOrderItem()) {
            if (item == null) {
                throw new InvalidProductOrderException("productOrderItem entries must not be null");
            }
            if (!itemIds.add(item.getId())) {
                throw new InvalidProductOrderException("Duplicate productOrderItem id: " + item.getId());
            }
            if (item.getAction() != OrderItemActionType.ADD) {
                throw new InvalidProductOrderException(
                        "productOrderItem " + item.getId() + ": only action 'add' is supported for mobile activation");
            }
            if (item.getQuantity() != null && item.getQuantity() != 1) {
                throw new InvalidProductOrderException(
                        "productOrderItem " + item.getId() + ": quantity must be 1 (one MSISDN per item)");
            }
            if (item.getProduct().getProductCharacteristic() != null
                    && item.getProduct().getProductCharacteristic().contains(null)) {
                throw new InvalidProductOrderException(
                        "productOrderItem " + item.getId() + ": productCharacteristic entries must not be null");
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

    public static Optional<String> customerId(List<RelatedParty> relatedParties) {
        if (relatedParties == null) {
            return Optional.empty();
        }
        return relatedParties.stream()
                .filter(Objects::nonNull)
                .filter(party -> CUSTOMER_ROLE.equalsIgnoreCase(party.getRole()) && !isBlank(party.getId()))
                .map(RelatedParty::getId)
                .findFirst();
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
