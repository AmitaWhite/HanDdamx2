package com.white.handdam.payment.client;

import com.white.handdam.payment.config.TossPaymentsProperties;
import com.white.handdam.payment.dto.toss.TossConfirmRequest;
import com.white.handdam.payment.dto.toss.TossConfirmResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TossPaymentsRestClientTest {

    private static final String BASE_URL = "https://api.tosspayments.com";
    private static final String SECRET_KEY = "test_sk_sample";
    private static final String IDEMPOTENCY_KEY = "idempotency-key";

    @Test
    @DisplayName("confirm sends Toss authorization and idempotency headers")
    void confirmSendsHeaders() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TossPaymentsRestClient client = new TossPaymentsRestClient(
                properties(),
                restClientBuilder
        );

        server.expect(once(), requestTo(BASE_URL + "/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
                .andExpect(header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "paymentKey": "payment-key",
                          "orderId": "HANDDAM-1234567890abcdef",
                          "amount": 15000
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "payment-key",
                          "orderId": "HANDDAM-1234567890abcdef",
                          "status": "DONE",
                          "totalAmount": 15000,
                          "method": "카드",
                          "approvedAt": "2026-07-15T01:00:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));

        TossConfirmResponse response = client.confirm(
                new TossConfirmRequest("payment-key", "HANDDAM-1234567890abcdef", 15000),
                IDEMPOTENCY_KEY
        );

        assertThat(response.status()).isEqualTo("DONE");
        assertThat(response.approvedAt()).isEqualTo(Instant.parse("2026-07-15T01:00:00Z"));
        server.verify();
    }

    @Test
    @DisplayName("confirm maps definitive 4xx Toss error to TossConfirmFailureException")
    void confirmMapsFourHundredError() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TossPaymentsRestClient client = new TossPaymentsRestClient(
                properties(),
                restClientBuilder
        );

        server.expect(once(), requestTo(BASE_URL + "/v1/payments/confirm"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "REJECT_CARD_COMPANY",
                                  "message": "카드사에서 결제를 거절했습니다."
                                }
                                """));

        assertThatThrownBy(() -> client.confirm(
                new TossConfirmRequest("payment-key", "HANDDAM-1234567890abcdef", 15000),
                IDEMPOTENCY_KEY
        ))
                .isInstanceOfSatisfying(
                        TossConfirmFailureException.class,
                        exception -> assertThat(exception.getFailureCode()).isEqualTo("REJECT_CARD_COMPANY")
                );
        server.verify();
    }

    @Test
    @DisplayName("confirm keeps 5xx Toss errors as uncertain API errors")
    void confirmMapsFiveHundredError() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TossPaymentsRestClient client = new TossPaymentsRestClient(
                properties(),
                restClientBuilder
        );

        server.expect(once(), requestTo(BASE_URL + "/v1/payments/confirm"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "BAD_GATEWAY",
                                  "message": "temporary error"
                                }
                                """));

        assertThatThrownBy(() -> client.confirm(
                new TossConfirmRequest("payment-key", "HANDDAM-1234567890abcdef", 15000),
                IDEMPOTENCY_KEY
        ))
                .isInstanceOf(TossApiException.class);
        server.verify();
    }

    private TossPaymentsProperties properties() {
        TossPaymentsProperties properties = new TossPaymentsProperties();
        properties.setSecretKey(SECRET_KEY);
        properties.setApiBaseUrl(BASE_URL);
        return properties;
    }

    private String authorizationHeader() {
        String encoded = Base64.getEncoder()
                .encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
