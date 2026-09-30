package com.telecom.activation.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductRefOrValue {

    private String id;

    private String href;

    private String name;

    private Boolean isBundle;

    @Valid
    private List<Characteristic> productCharacteristic = new ArrayList<>();

    @JsonProperty("@type")
    private String type;

    public Optional<String> characteristicValue(String characteristicName) {
        if (productCharacteristic == null) {
            return Optional.empty();
        }
        return productCharacteristic.stream()
                .filter(c -> characteristicName.equalsIgnoreCase(c.getName()))
                .map(Characteristic::getValue)
                .filter(value -> value != null)
                .map(value -> value.toString().trim())
                .findFirst();
    }
}
