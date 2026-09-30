package com.telecom.activation.fulfillment;

import com.telecom.activation.api.model.Note;
import com.telecom.activation.api.model.ProductOrder;
import com.telecom.activation.api.model.ProductOrderErrorMessage;
import com.telecom.activation.api.model.ProductOrderItem;
import com.telecom.activation.api.model.ProductOrderItemStateType;
import com.telecom.activation.api.model.ProductOrderStateType;
import com.telecom.activation.fulfillment.model.ActivationContext;
import com.telecom.activation.repository.ProductOrderRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFulfillmentTracker {

    private static final String AUTHOR = "mobile-activation-service";

    private final ProductOrderRepository repository;

    public void markInProgress(Exchange exchange) {
        ProductOrder order = order(exchange);
        order.setState(ProductOrderStateType.IN_PROGRESS);
        order.getProductOrderItem().forEach(item -> item.setState(ProductOrderItemStateType.IN_PROGRESS));
        addNote(order, "Fulfillment started");
        exchange.setProperty(MobileActivationRoutes.ORDER, order);
        exchange.getMessage().setBody(order);
    }

    public void recordStepCompleted(Exchange exchange) {
        ActivationContext activation = activation(exchange);
        String step = exchange.getProperty(MobileActivationRoutes.STEP, String.class);
        String reference = exchange.getProperty(MobileActivationRoutes.STEP_REFERENCE, String.class);
        addNote(order(exchange), String.format("Item %s (MSISDN %s): %s step completed [%s]",
                activation.getOrderItemId(), activation.getMsisdn(), step, reference));
    }

    public void recordStepFailure(Exchange exchange) {
        ProductOrder order = order(exchange);
        ActivationContext activation = activation(exchange);
        String step = exchange.getProperty(MobileActivationRoutes.STEP, String.class);
        Throwable cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        String message = describe(cause);

        item(order, activation.getOrderItemId()).setState(ProductOrderItemStateType.FAILED);
        order.getErrorMessage().add(new ProductOrderErrorMessage(
                step + "_FAILED", step + " system call failed", message, activation.getOrderItemId(), now()));
        addNote(order, String.format("Item %s (MSISDN %s): %s step failed [%s]",
                activation.getOrderItemId(), activation.getMsisdn(), step, message));
        log.error("ProductOrder {} item {} failed at {} step: {}", order.getId(), activation.getOrderItemId(), step, message);
    }

    public void markItemCompleted(Exchange exchange) {
        ActivationContext activation = activation(exchange);
        item(order(exchange), activation.getOrderItemId()).setState(ProductOrderItemStateType.COMPLETED);
    }

    public void markCompleted(Exchange exchange) {
        ProductOrder order = order(exchange);
        order.setCompletionDate(now());
        order.setState(ProductOrderStateType.COMPLETED);
        addNote(order, "Mobile activation completed");
    }

    public void markFailed(Exchange exchange) {
        ProductOrder order = order(exchange);
        Throwable cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

        boolean anyCompleted = false;
        boolean anyItemFailed = false;
        for (ProductOrderItem item : order.getProductOrderItem()) {
            if (item.getState() == ProductOrderItemStateType.COMPLETED) {
                anyCompleted = true;
            } else if (item.getState() == ProductOrderItemStateType.FAILED) {
                anyItemFailed = true;
            } else {
                item.setState(ProductOrderItemStateType.CANCELLED);
            }
        }
        if (!anyItemFailed) {
            order.getErrorMessage().add(new ProductOrderErrorMessage(
                    "FULFILLMENT_FAILED", "Fulfillment failed", describe(cause), null, now()));
        }
        order.setCompletionDate(now());
        order.setState(anyCompleted ? ProductOrderStateType.PARTIAL : ProductOrderStateType.FAILED);
        addNote(order, "Mobile activation " + order.getState().getValue());
    }

    private ProductOrder order(Exchange exchange) {
        String orderId = exchange.getProperty(MobileActivationRoutes.ORDER_ID, String.class);
        return repository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("ProductOrder not found: " + orderId));
    }

    private static ActivationContext activation(Exchange exchange) {
        return exchange.getProperty(MobileActivationRoutes.ACTIVATION, ActivationContext.class);
    }

    private static ProductOrderItem item(ProductOrder order, String itemId) {
        return order.getProductOrderItem().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ProductOrderItem not found: " + itemId));
    }

    private static void addNote(ProductOrder order, String text) {
        order.getNote().add(new Note(UUID.randomUUID().toString(), AUTHOR, now(), text));
    }

    private static String describe(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpOperationFailedException http) {
                return "HTTP " + http.getStatusCode() + " from " + http.getUri() + ": " + http.getResponseBody();
            }
            if (current.getCause() == null || current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return current == null ? "unknown error" : current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
