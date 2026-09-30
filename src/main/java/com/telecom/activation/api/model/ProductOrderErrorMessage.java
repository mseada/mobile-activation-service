package com.telecom.activation.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOrderErrorMessage {

    private String code;

    private String reason;

    private String message;

    private String productOrderItemId;

    private OffsetDateTime timestamp;
}
