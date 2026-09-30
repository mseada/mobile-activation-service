package com.telecom.activation.repository;

import com.telecom.activation.api.model.ProductOrder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class ProductOrderRepository {

    private final Map<String, ProductOrder> orders = new ConcurrentHashMap<>();

    public ProductOrder save(ProductOrder order) {
        orders.put(order.getId(), order);
        return order;
    }

    public Optional<ProductOrder> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public List<ProductOrder> findAll() {
        return orders.values().stream()
                .sorted(Comparator.comparing(ProductOrder::getOrderDate).reversed())
                .toList();
    }
}
