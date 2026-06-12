package com.fundflow.web;

import com.fundflow.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack HTTP tests: controller routing, validation, the global error
 * shape, and status codes — against a real PostgreSQL via Testcontainers.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.sql.init.mode=never",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@Import(TestcontainersConfiguration.class)
class ApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForMap(String url, String body) {
        return rest.postForObject(url, json(body), Map.class);
    }

    @Test
    void endToEndOverHttp_createIssuePay() {
        Map<String, Object> fund = postForMap("/api/v1/funds", """
                {"name": "HTTP Fund", "vintageYear": 2026, "targetSize": 1000000, "currency": "USD"}""");
        Integer fundId = (Integer) fund.get("id");
        assertThat(fundId).isNotNull();

        Map<String, Object> investor = postForMap("/api/v1/investors", """
                {"name": "HTTP LP", "email": "http@lp.example"}""");
        Integer investorId = (Integer) investor.get("id");

        postForMap("/api/v1/funds/" + fundId + "/commitments", """
                {"investorId": %d, "amount": 1000000}""".formatted(investorId));

        ResponseEntity<Map> created = rest.postForEntity("/api/v1/funds/" + fundId + "/capital-calls",
                json("""
                        {"totalAmount": 50000, "dueDate": "2030-01-15"}"""), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Integer callId = (Integer) created.getBody().get("id");
        assertThat(created.getBody().get("status")).isEqualTo("DRAFT");

        Map<String, Object> issued = postForMap("/api/v1/capital-calls/" + callId + "/issue", "");
        assertThat(issued.get("status")).isEqualTo("ISSUED");

        var allocations = (java.util.List<Map<String, Object>>) issued.get("allocations");
        Integer allocationId = (Integer) allocations.get(0).get("id");
        Map<String, Object> paid = postForMap("/api/v1/allocations/" + allocationId + "/pay", "");
        assertThat(paid.get("paid")).isEqualTo(true);

        Map<String, Object> summary = rest.getForObject("/api/v1/funds/" + fundId + "/summary", Map.class);
        assertThat(summary.get("totalCalled")).isEqualTo(50000.0);
        assertThat(summary.get("totalPaid")).isEqualTo(50000.0);
    }

    @Test
    void validationErrorsReturn400WithFieldDetails() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/funds",
                json("""
                        {"name": "", "vintageYear": 1700, "targetSize": -5, "currency": "usd"}"""), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> fieldErrors = (Map<String, Object>) response.getBody().get("fieldErrors");
        assertThat(fieldErrors).containsKeys("name", "vintageYear", "targetSize", "currency");
    }

    @Test
    void unknownFundReturns404WithErrorBody() {
        ResponseEntity<Map> response = rest.getForEntity("/api/v1/funds/424242", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("message")).isEqualTo("Fund with id 424242 not found");
    }

    @Test
    void issuingTwiceReturns409Conflict() {
        Map<String, Object> fund = postForMap("/api/v1/funds", """
                {"name": "Conflict Fund", "vintageYear": 2026, "targetSize": 1000000, "currency": "USD"}""");
        Integer fundId = (Integer) fund.get("id");
        Map<String, Object> investor = postForMap("/api/v1/investors", """
                {"name": "Conflict LP", "email": "conflict@lp.example"}""");
        postForMap("/api/v1/funds/" + fundId + "/commitments", """
                {"investorId": %d, "amount": 500000}""".formatted((Integer) investor.get("id")));
        Map<String, Object> call = postForMap("/api/v1/funds/" + fundId + "/capital-calls", """
                {"totalAmount": 1000, "dueDate": "2030-01-15"}""");
        Integer callId = (Integer) call.get("id");

        postForMap("/api/v1/capital-calls/" + callId + "/issue", "");
        ResponseEntity<Map> second = rest.postForEntity(
                "/api/v1/capital-calls/" + callId + "/issue", json(""), Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) second.getBody().get("message")).contains("cannot be issued");
    }

    @Test
    void malformedJsonReturns400WithApiErrorShape() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/funds",
                json("{not valid json"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("Malformed request body");
    }
}
