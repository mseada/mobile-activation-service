package com.telecom.activation.api.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProductOrderStateType {
    ACKNOWLEDGED("acknowledged"),
    REJECTED("rejected"),
    PENDING("pending"),
    HELD("held"),
    IN_PROGRESS("inProgress"),
    CANCELLED("cancelled"),
    COMPLETED("completed"),
    FAILED("failed"),
    PARTIAL("partial");

    private final String value;

    ProductOrderStateType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static ProductOrderStateType fromValue(String value) {
        for (ProductOrderStateType state : values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown product order state: " + value);
    }
}
