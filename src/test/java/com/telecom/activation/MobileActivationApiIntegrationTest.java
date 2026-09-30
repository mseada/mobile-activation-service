package com.telecom.activation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.activation.stub.StubInvocation;
import com.telecom.activation.stub.StubRegistry;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class MobileActivationApiIntegrationTest {

    private static final String ORDERS = "/tmf-api/productOrderingManagement/v4/productOrder";
    private static final int PORT = freePort();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
        registry.add("integration.retry.redelivery-delay-ms", () -> 10);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubRegistry stubRegistry;

    @BeforeEach
    void resetStubs() {
        stubRegistry.clear();
    }

    @Test
    void activatesMobileLineThroughBillingChargingCrmAndKafka() {
        ResponseEntity<JsonNode> created = post(order("201012345678", "89201012345678901234"));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = created.getBody().get("id").asText();
        assertThat(created.getHeaders().getLocation()).hasToString(created.getBody().get("href").asText());
        assertThat(created.getBody().get("state").asText()).isIn("acknowledged", "inProgress", "completed");

        JsonNode order = awaitFinalState(id);
        assertThat(order.get("state").asText()).isEqualTo("completed");
        assertThat(order.get("completionDate")).isNotNull();
        assertThat(order.get("productOrderItem").get(0).get("state").asText()).isEqualTo("completed");
        assertThat(order.has("errorMessage") && order.get("errorMessage").size() > 0).isFalse();

        assertThat(stubRegistry.invocations()).extracting(StubInvocation::system)
                .containsExactly("billing", "charging", "crm", "kafka");

        assertThat(stubRegistry.events()).hasSize(1);
        JsonNode event = stubRegistry.events().get(0).payload();
        assertThat(stubRegistry.events().get(0).key()).isEqualTo("201012345678");
        assertThat(event.get("eventType").asText()).isEqualTo("MobileLineActivatedEvent");
        assertThat(event.get("orderId").asText()).isEqualTo(id);
        assertThat(event.get("msisdn").asText()).isEqualTo("201012345678");
        assertThat(event.get("customerId").asText()).isEqualTo("CUST-1001");
        assertThat(event.get("billingSubscriptionId").asText()).startsWith("SUB-");
        assertThat(event.get("chargingSubscriberId").asText()).startsWith("OCS-");
        assertThat(event.get("crmAssetId").asText()).startsWith("AST-");
    }

    @Test
    void failsOrderAndStopsFulfillmentWhenChargingSystemIsDown() {
        String failingMsisdn = "201000000002";
        ResponseEntity<JsonNode> created = post(order(failingMsisdn, "89201012345678901235"));
        String id = created.getBody().get("id").asText();

        JsonNode order = awaitFinalState(id);
        assertThat(order.get("state").asText()).isEqualTo("failed");
        assertThat(order.get("productOrderItem").get(0).get("state").asText()).isEqualTo("failed");
        JsonNode error = order.get("errorMessage").get(0);
        assertThat(error.get("code").asText()).isEqualTo("CHARGING_FAILED");
        assertThat(error.get("message").asText()).contains("HTTP 503");

        List<String> systems = stubRegistry.invocations().stream().map(StubInvocation::system).toList();
        assertThat(systems).containsExactly("billing", "charging", "charging", "charging");
        assertThat(stubRegistry.events()).isEmpty();
    }

    @Test
    void marksOrderPartialWhenLaterItemFails() throws IOException {
        JsonNode request = objectMapper.readTree(order("201012345670", "89201012345678901236"));
        JsonNode secondItem = objectMapper.readTree(order("201000000003", "89201012345678901237"))
                .get("productOrderItem").get(0);
        ((com.fasterxml.jackson.databind.node.ObjectNode) secondItem).put("id", "2");
        ((com.fasterxml.jackson.databind.node.ArrayNode) request.get("productOrderItem")).add(secondItem);

        String id = post(request.toString()).getBody().get("id").asText();

        JsonNode order = awaitFinalState(id);
        assertThat(order.get("state").asText()).isEqualTo("partial");
        assertThat(order.get("productOrderItem").get(0).get("state").asText()).isEqualTo("completed");
        assertThat(order.get("productOrderItem").get(1).get("state").asText()).isEqualTo("failed");
        assertThat(order.get("errorMessage").get(0).get("code").asText()).isEqualTo("CRM_FAILED");
        assertThat(stubRegistry.events()).hasSize(1);
    }

    @Test
    void rejectsOrderWithoutMsisdn() {
        String body = order("201012345678", "89201012345678901234").replace("\"MSISDN\"", "\"OTHER\"");

        ResponseEntity<JsonNode> response = post(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").asText()).contains("MSISDN");
        assertThat(stubRegistry.invocations()).isEmpty();
    }

    @Test
    void rejectsOrderWithoutCustomer() {
        String body = order("201012345678", "89201012345678901234").replace("\"Customer\"", "\"Agent\"");

        ResponseEntity<JsonNode> response = post(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").asText()).contains("Customer");
    }

    @Test
    void retriesKafkaPublishWhenBrokerIsDown() {
        ResponseEntity<JsonNode> created = post(order("201000000004", "89201012345678901236"));

        JsonNode order = awaitFinalState(created.getBody().get("id").asText());
        assertThat(order.get("state").asText()).isEqualTo("failed");
        assertThat(order.get("errorMessage").get(0).get("code").asText()).isEqualTo("KAFKA_FAILED");
        List<String> systems = stubRegistry.invocations().stream().map(StubInvocation::system).toList();
        assertThat(systems).containsExactly("billing", "charging", "crm", "kafka", "kafka", "kafka");
    }

    @Test
    void usesFirstCustomerWithIdWhenEarlierCustomerEntryIsBlank() {
        String body = order("201012345679", "89201012345678901235").replace(
                "{ \"id\": \"CUST-1001\"",
                "{ \"role\": \"Customer\" }, { \"id\": \"CUST-1001\"");

        ResponseEntity<JsonNode> response = post(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode order = awaitFinalState(response.getBody().get("id").asText());
        assertThat(order.get("state").asText()).isEqualTo("completed");
    }

    @Test
    void rejectsInvalidOrdersWithBadRequest() {
        String valid = order("201012345678", "89201012345678901234");
        List<String> invalidBodies = List.of(
                valid.replace("\"quantity\": 1", "\"quantity\": 2"),
                valid.replace("\"CUST-1001\"", "\"CUST/1001?x=1\""),
                valid.replace("\"relatedParty\": [", "\"relatedParty\": [ null,"),
                valid.replace("\"productOrderItem\": [", "\"productOrderItem\": [ null,"),
                valid.replace("\"productCharacteristic\": [", "\"productCharacteristic\": [ null,"));

        for (String body : invalidBodies) {
            assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(stubRegistry.invocations()).isEmpty();
    }

    @Test
    void returnsNotFoundForUnknownOrder() {
        ResponseEntity<JsonNode> response = rest.getForEntity(ORDERS + "/does-not-exist", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private JsonNode awaitFinalState(String id) {
        return await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(100))
                .until(() -> rest.getForObject(ORDERS + "/" + id, JsonNode.class),
                        order -> List.of("completed", "failed", "partial").contains(order.get("state").asText()));
    }

    private ResponseEntity<JsonNode> post(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(ORDERS, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private static String order(String msisdn, String iccid) {
        return """
                {
                  "externalId": "SO-%s",
                  "description": "New mobile line activation",
                  "category": "MobileActivation",
                  "relatedParty": [
                    { "id": "CUST-1001", "name": "Jane Doe", "role": "Customer", "@referredType": "Individual" }
                  ],
                  "productOrderItem": [
                    {
                      "id": "1",
                      "action": "add",
                      "quantity": 1,
                      "productOffering": { "id": "PO-PREPAID-5G", "name": "Prepaid 5G" },
                      "product": {
                        "name": "Mobile Line",
                        "productCharacteristic": [
                          { "name": "MSISDN", "valueType": "string", "value": "%s" },
                          { "name": "ICCID", "valueType": "string", "value": "%s" }
                        ]
                      }
                    }
                  ],
                  "@type": "ProductOrder"
                }
                """.formatted(msisdn, msisdn, iccid);
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
