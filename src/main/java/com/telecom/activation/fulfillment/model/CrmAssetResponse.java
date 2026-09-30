package com.telecom.activation.fulfillment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CrmAssetResponse(
        String assetId,
        String status) {
}
