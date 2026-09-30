package com.telecom.activation.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String reason,
        String message,
        String status,
        @JsonProperty("@type") String type) {

    public static ErrorResponse of(int status, String reason, String message) {
        return new ErrorResponse(String.valueOf(status), reason, message, String.valueOf(status), "Error");
    }
}
