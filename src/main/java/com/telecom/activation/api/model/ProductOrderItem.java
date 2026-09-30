package com.telecom.activation.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOrderItem {

    @NotBlank
    private String id;

    private Integer quantity;

    @NotNull
    private OrderItemActionType action;

    private ProductOrderItemStateType state;

    @Valid
    @NotNull
    private ProductRefOrValue product;

    private ProductOfferingRef productOffering;

    @JsonProperty("@type")
    private String type;
}
