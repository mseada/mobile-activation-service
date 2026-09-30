package com.telecom.activation.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOrderCreate {

    private String externalId;

    private String description;

    private String category;

    private String priority;

    private OffsetDateTime requestedStartDate;

    private OffsetDateTime requestedCompletionDate;

    private BillingAccountRef billingAccount;

    @Valid
    @NotEmpty
    private List<ProductOrderItem> productOrderItem = new ArrayList<>();

    private List<RelatedParty> relatedParty = new ArrayList<>();

    private List<Note> note = new ArrayList<>();

    @JsonProperty("@type")
    private String type;
}
