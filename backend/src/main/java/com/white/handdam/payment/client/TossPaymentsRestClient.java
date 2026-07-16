package com.white.handdam.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.white.handdam.payment.config.TossPaymentsProperties;
import com.white.handdam.payment.dto.toss.TossConfirmRequest;
import com.white.handdam.payment.dto.toss.TossConfirmResponse;
import com.white.handdam.payment.dto.toss.TossErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TossPaymentsRestClient implements TossPaymentsClient {

    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String IDEMPOTENT_REQUEST_PROCESSING = "IDEMPOTENT_REQUEST_PROCESSING";
    private static final String UNKNOWN_TOSS_ERROR = "UNKNOWN_TOSS_ERROR";
    private static final String TOSS_COMMUNICATION_ERROR = "TOSS_COMMUNICATION_ERROR";
    private static final String TOSS_TIMEOUT = "TOSS_TIMEOUT";

    private final TossPaymentsProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public TossPaymentsRestClient(
            TossPaymentsProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.restClient = restClientBuilder
                .baseUrl(properties.getApiBaseUrl())
                .build();
    }

    @Override
    public TossConfirmResponse confirm(TossConfirmRequest request, String idempotencyKey) {
        try {
            TossConfirmResponse response = restClient.post()
                    .uri(CONFIRM_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, clientResponse) -> {
                        TossErrorResponse errorResponse = readErrorResponse(clientResponse);
                        if (clientResponse.getStatusCode().is5xxServerError()
                                || IDEMPOTENT_REQUEST_PROCESSING.equals(errorResponse.code())) {
                            throw new TossApiException(errorResponse.code(), errorResponse.message());
                        }
                        throw new TossConfirmFailureException(errorResponse.code(), errorResponse.message());
                    })
                    .body(TossConfirmResponse.class);

            if (response == null) {
                throw new TossApiException(UNKNOWN_TOSS_ERROR, "Toss Payments response body is empty.");
            }

            return response;
        } catch (TossPaymentException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new TossTimeoutException(TOSS_TIMEOUT, "Toss Payments request timed out.");
        } catch (RestClientException exception) {
            throw new TossApiException(TOSS_COMMUNICATION_ERROR, "Toss Payments request failed.");
        }
    }

    private String authorizationHeader() {
        String secretKey = properties.getSecretKey();
        if (!StringUtils.hasText(secretKey)) {
            throw new TossApiException(TOSS_COMMUNICATION_ERROR, "Toss Payments secret key is not configured.");
        }

        String credential = secretKey + ":";
        String encodedCredential = Base64.getEncoder()
                .encodeToString(credential.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredential;
    }

    private TossErrorResponse readErrorResponse(org.springframework.http.client.ClientHttpResponse response)
            throws IOException {
        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        if (!StringUtils.hasText(body)) {
            return new TossErrorResponse(UNKNOWN_TOSS_ERROR, "Toss Payments error response body is empty.");
        }

        try {
            TossErrorResponse errorResponse = objectMapper.readValue(body, TossErrorResponse.class);
            return new TossErrorResponse(
                    defaultIfBlank(errorResponse.code(), UNKNOWN_TOSS_ERROR),
                    defaultIfBlank(errorResponse.message(), "Toss Payments request failed.")
            );
        } catch (IOException exception) {
            return new TossErrorResponse(UNKNOWN_TOSS_ERROR, "Toss Payments error response is invalid.");
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        return defaultValue;
    }
}
