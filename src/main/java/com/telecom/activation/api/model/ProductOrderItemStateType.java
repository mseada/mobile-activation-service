package com.telecom.activation.api.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProductOrderItemStateType {
    ACKNOWLEDGED("acknowledged"),
    REJECTED("rejected"),
    PENDING("pending"),
    HELD("held"),
    IN_PROGRESS("inProgress"),
    CANCELLED("cancelled"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    ProductOrderItemStateType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
