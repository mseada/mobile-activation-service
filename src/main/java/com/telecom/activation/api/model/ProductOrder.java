package com.telecom.activation.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOrder {

    private String id;

    private String href;

    private String externalId;

    private String description;

    private String category;

    private String priority;

    private volatile ProductOrderStateType state;

    private OffsetDateTime orderDate;

    private volatile OffsetDateTime completionDate;

    private OffsetDateTime requestedStartDate;

    private OffsetDateTime requestedCompletionDate;

    private BillingAccountRef billingAccount;

    private List<ProductOrderItem> productOrderItem = new CopyOnWriteArrayList<>();

    private List<RelatedParty> relatedParty = new CopyOnWriteArrayList<>();

    private List<Note> note = new CopyOnWriteArrayList<>();

    private List<ProductOrderErrorMessage> errorMessage = new CopyOnWriteArrayList<>();

    @JsonProperty("@type")
    private String type = "ProductOrder";
}
