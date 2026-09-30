package com.telecom.activation.service;

import com.telecom.activation.api.model.ProductOrder;
import com.telecom.activation.api.model.ProductOrderCreate;
import com.telecom.activation.api.model.ProductOrderItemStateType;
import com.telecom.activation.api.model.ProductOrderStateType;
import com.telecom.activation.fulfillment.MobileActivationRoutes;
import com.telecom.activation.repository.ProductOrderRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductOrderService {

    private final ProductOrderRepository repository;
    private final MobileActivationOrderValidator validator;
    private final ProducerTemplate producerTemplate;

    public ProductOrder create(ProductOrderCreate request, String baseHref) {
        validator.validate(request);

        ProductOrder order = new ProductOrder();
        order.setId(UUID.randomUUID().toString());
        order.setHref(baseHref + "/" + order.getId());
        order.setExternalId(request.getExternalId());
        order.setDescription(request.getDescription());
        order.setCategory(request.getCategory());
        order.setPriority(request.getPriority());
        order.setRequestedStartDate(request.getRequestedStartDate());
        order.setRequestedCompletionDate(request.getRequestedCompletionDate());
        order.setBillingAccount(request.getBillingAccount());
        order.setOrderDate(OffsetDateTime.now(ZoneOffset.UTC));
        order.setState(ProductOrderStateType.ACKNOWLEDGED);
        request.getProductOrderItem().forEach(item -> {
            item.setState(ProductOrderItemStateType.ACKNOWLEDGED);
            order.getProductOrderItem().add(item);
        });
        if (request.getRelatedParty() != null) {
            order.getRelatedParty().addAll(request.getRelatedParty());
        }
        if (request.getNote() != null) {
            order.getNote().addAll(request.getNote());
        }

        repository.save(order);
        log.info("ProductOrder {} acknowledged with {} item(s)", order.getId(), order.getProductOrderItem().size());

        producerTemplate.sendBody(MobileActivationRoutes.ORDER_FULFILLMENT_ENDPOINT, order.getId());
        return order;
    }

    public ProductOrder get(String id) {
        return repository.findById(id).orElseThrow(() -> new ProductOrderNotFoundException(id));
    }

    public List<ProductOrder> list(ProductOrderStateType state) {
        return repository.findAll().stream()
                .filter(order -> state == null || order.getState() == state)
                .toList();
    }
}
