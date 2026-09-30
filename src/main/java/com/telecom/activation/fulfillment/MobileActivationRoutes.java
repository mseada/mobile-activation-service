package com.telecom.activation.fulfillment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.activation.api.model.ProductOrder;
import com.telecom.activation.api.model.ProductOrderItem;
import com.telecom.activation.fulfillment.model.ActivationContext;
import com.telecom.activation.fulfillment.model.BillingSubscriptionRequest;
import com.telecom.activation.fulfillment.model.BillingSubscriptionResponse;
import com.telecom.activation.fulfillment.model.ChargingSubscriberRequest;
import com.telecom.activation.fulfillment.model.ChargingSubscriberResponse;
import com.telecom.activation.fulfillment.model.CrmAssetRequest;
import com.telecom.activation.fulfillment.model.CrmAssetResponse;
import com.telecom.activation.fulfillment.model.MobileLineActivatedEvent;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fulfillment of TMF622 mobile activation orders:
 * billing -> charging -> CRM -> Kafka event, executed sequentially for every order item.
 */
@Component
public class MobileActivationRoutes extends RouteBuilder {

    public static final String ORDER_FULFILLMENT_ENDPOINT = "seda:productOrderFulfillment";
    public static final String ACTIVATE_MOBILE_LINE_ENDPOINT = "direct:activateMobileLine";
    public static final String BILLING_ENDPOINT = "direct:billingSystem";
    public static final String CHARGING_ENDPOINT = "direct:chargingSystem";
    public static final String CRM_ENDPOINT = "direct:crmSystem";
    public static final String PUBLISH_EVENT_ENDPOINT = "direct:publishActivationEvent";

    public static final String ORDER_ID = "orderId";
    public static final String ORDER = "order";
    public static final String ACTIVATION = "activation";
    public static final String STEP = "fulfillmentStep";
    public static final String STEP_REFERENCE = "fulfillmentStepReference";

    private final OrderFulfillmentTracker tracker;
    private final ActivationContextFactory activationContextFactory;
    private final ObjectMapper objectMapper;
    private final int maximumRedeliveries;
    private final long redeliveryDelayMs;

    public MobileActivationRoutes(
            OrderFulfillmentTracker tracker,
            ActivationContextFactory activationContextFactory,
            ObjectMapper objectMapper,
            @Value("${integration.retry.maximum-redeliveries:2}") int maximumRedeliveries,
            @Value("${integration.retry.redelivery-delay-ms:500}") long redeliveryDelayMs) {
        this.tracker = tracker;
        this.activationContextFactory = activationContextFactory;
        this.objectMapper = objectMapper;
        this.maximumRedeliveries = maximumRedeliveries;
        this.redeliveryDelayMs = redeliveryDelayMs;
    }

    @Override
    public void configure() {
        from(ORDER_FULFILLMENT_ENDPOINT + "?concurrentConsumers={{fulfillment.concurrent-consumers:5}}")
                .routeId("product-order-fulfillment")
                .setProperty(ORDER_ID, body())
                .log("Starting fulfillment of ProductOrder ${exchangeProperty.orderId}")
                .process(tracker::markInProgress)
                .doTry()
                    .split(simple("${body.productOrderItem}")).stopOnException()
                        .to(ACTIVATE_MOBILE_LINE_ENDPOINT)
                    .end()
                    .process(tracker::markCompleted)
                    .log("ProductOrder ${exchangeProperty.orderId} completed")
                .endDoTry()
                .doCatch(Exception.class)
                    .process(tracker::markFailed)
                    .log(LoggingLevel.ERROR, "ProductOrder ${exchangeProperty.orderId} fulfillment failed: ${exception.message}")
                .end();

        from(ACTIVATE_MOBILE_LINE_ENDPOINT)
                .routeId("mobile-line-activation")
                .process(exchange -> {
                    ProductOrder order = exchange.getProperty(ORDER, ProductOrder.class);
                    ProductOrderItem item = exchange.getMessage().getBody(ProductOrderItem.class);
                    exchange.setProperty(ACTIVATION, activationContextFactory.create(order, item));
                })
                .log("Activating MSISDN ${exchangeProperty.activation.msisdn} "
                        + "(order ${exchangeProperty.orderId}, item ${exchangeProperty.activation.orderItemId})")
                .to(BILLING_ENDPOINT)
                .to(CHARGING_ENDPOINT)
                .to(CRM_ENDPOINT)
                .to(PUBLISH_EVENT_ENDPOINT)
                .process(tracker::markItemCompleted);

        step(BILLING_ENDPOINT, "billing-system-step", "BILLING")
                .process(exchange -> {
                    ActivationContext ctx = activation(exchange);
                    exchange.getMessage().setBody(new BillingSubscriptionRequest(
                            ctx.getOrderId(), ctx.getCustomerId(), ctx.getBillingAccountId(),
                            ctx.getMsisdn(), ctx.getProductOfferingId()));
                })
                .marshal(json(Object.class))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("{{integration.billing.url}}/api/v1/subscriptions")
                .unmarshal(json(BillingSubscriptionResponse.class))
                .process(exchange -> {
                    BillingSubscriptionResponse response = exchange.getMessage().getBody(BillingSubscriptionResponse.class);
                    ActivationContext ctx = activation(exchange);
                    ctx.setBillingSubscriptionId(response.subscriptionId());
                    ctx.setBillingAccountId(response.billingAccountId());
                    exchange.setProperty(STEP_REFERENCE, "billingAccountId=" + response.billingAccountId()
                            + ", subscriptionId=" + response.subscriptionId());
                })
                .process(tracker::recordStepCompleted);

        step(CHARGING_ENDPOINT, "charging-system-step", "CHARGING")
                .process(exchange -> {
                    ActivationContext ctx = activation(exchange);
                    exchange.getMessage().setBody(new ChargingSubscriberRequest(
                            ctx.getMsisdn(), ctx.getIccid(), ctx.getProductOfferingId(), ctx.getBillingSubscriptionId()));
                })
                .marshal(json(Object.class))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("{{integration.charging.url}}/api/v1/subscribers")
                .unmarshal(json(ChargingSubscriberResponse.class))
                .process(exchange -> {
                    ChargingSubscriberResponse response = exchange.getMessage().getBody(ChargingSubscriberResponse.class);
                    activation(exchange).setChargingSubscriberId(response.subscriberId());
                    exchange.setProperty(STEP_REFERENCE, "subscriberId=" + response.subscriberId());
                })
                .process(tracker::recordStepCompleted);

        step(CRM_ENDPOINT, "crm-system-step", "CRM")
                .process(exchange -> {
                    ActivationContext ctx = activation(exchange);
                    exchange.getMessage().setBody(new CrmAssetRequest(
                            ctx.getOrderId(), ctx.getMsisdn(), ctx.getIccid(), ctx.getProductOfferingId(),
                            ctx.getBillingSubscriptionId(), ctx.getChargingSubscriberId()));
                })
                .marshal(json(Object.class))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .toD("{{integration.crm.url}}/api/v1/customers/${exchangeProperty.activation.customerId}/assets")
                .unmarshal(json(CrmAssetResponse.class))
                .process(exchange -> {
                    CrmAssetResponse response = exchange.getMessage().getBody(CrmAssetResponse.class);
                    activation(exchange).setCrmAssetId(response.assetId());
                    exchange.setProperty(STEP_REFERENCE, "assetId=" + response.assetId());
                })
                .process(tracker::recordStepCompleted);

        step(PUBLISH_EVENT_ENDPOINT, "kafka-event-step", "KAFKA")
                .process(exchange -> {
                    ActivationContext ctx = activation(exchange);
                    MobileLineActivatedEvent event = new MobileLineActivatedEvent(
                            UUID.randomUUID().toString(), MobileLineActivatedEvent.EVENT_TYPE,
                            OffsetDateTime.now(ZoneOffset.UTC), ctx.getOrderId(), ctx.getOrderItemId(),
                            ctx.getCustomerId(), ctx.getMsisdn(), ctx.getIccid(), ctx.getProductOfferingId(),
                            ctx.getBillingAccountId(), ctx.getBillingSubscriptionId(), ctx.getChargingSubscriberId(),
                            ctx.getCrmAssetId());
                    exchange.getMessage().setBody(event);
                    exchange.setProperty(STEP_REFERENCE, "eventId=" + event.eventId());
                })
                .marshal(json(Object.class))
                .convertBodyTo(String.class)
                .setHeader(KafkaConstants.KEY, simple("${exchangeProperty.activation.msisdn}"))
                .to("{{integration.kafka.endpoint}}")
                .process(tracker::recordStepCompleted);
    }

    /**
     * Declares a fulfillment step route. Transient failures (I/O errors and HTTP 5xx) are retried;
     * once retries are exhausted, or for non-retryable errors, the failure is recorded on the order
     * and propagated so the order-level route stops processing.
     */
    private ProcessorDefinition<?> step(String endpoint, String routeId, String stepName) {
        return from(endpoint)
                .routeId(routeId)
                .onException(IOException.class)
                    .maximumRedeliveries(maximumRedeliveries)
                    .redeliveryDelay(redeliveryDelayMs)
                    .retryAttemptedLogLevel(LoggingLevel.WARN)
                    .process(tracker::recordStepFailure)
                .end()
                .onException(HttpOperationFailedException.class)
                    .onWhen(exchange -> isServerError(exchange))
                    .maximumRedeliveries(maximumRedeliveries)
                    .redeliveryDelay(redeliveryDelayMs)
                    .retryAttemptedLogLevel(LoggingLevel.WARN)
                    .process(tracker::recordStepFailure)
                .end()
                .onException(Exception.class)
                    .process(tracker::recordStepFailure)
                .end()
                .setProperty(STEP, constant(stepName))
                .removeHeaders("*")
                .log("${exchangeProperty.fulfillmentStep} step for MSISDN ${exchangeProperty.activation.msisdn}");
    }

    private static boolean isServerError(Exchange exchange) {
        Throwable exception = exchange.getException() != null
                ? exchange.getException()
                : exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        return exception instanceof HttpOperationFailedException http && http.getStatusCode() >= 500;
    }

    private static ActivationContext activation(Exchange exchange) {
        return exchange.getProperty(ACTIVATION, ActivationContext.class);
    }

    private JacksonDataFormat json(Class<?> type) {
        return new JacksonDataFormat(objectMapper, type);
    }
}
