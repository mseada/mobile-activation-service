package com.telecom.activation.api;

import com.telecom.activation.api.model.ProductOrder;
import com.telecom.activation.api.model.ProductOrderCreate;
import com.telecom.activation.api.model.ProductOrderStateType;
import com.telecom.activation.service.ProductOrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = ProductOrderController.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductOrderController {

    public static final String BASE_PATH = "/tmf-api/productOrderingManagement/v4/productOrder";

    private final ProductOrderService productOrderService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductOrder> createProductOrder(@Valid @RequestBody ProductOrderCreate productOrder) {
        String collectionHref = ServletUriComponentsBuilder.fromCurrentRequestUri().replaceQuery(null).toUriString();
        ProductOrder created = productOrderService.create(productOrder, collectionHref);
        return ResponseEntity.created(URI.create(created.getHref())).body(created);
    }

    @GetMapping
    public List<ProductOrder> listProductOrders(@RequestParam(required = false) String state) {
        return productOrderService.list(state == null ? null : ProductOrderStateType.fromValue(state));
    }

    @GetMapping("/{id}")
    public ProductOrder retrieveProductOrder(@PathVariable String id) {
        return productOrderService.get(id);
    }
}
