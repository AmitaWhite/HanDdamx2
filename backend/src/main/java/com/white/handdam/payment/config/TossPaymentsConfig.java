package com.white.handdam.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TossPaymentsConfig {

    @Bean("tossApiRestClient")
    public RestClient tossApiRestClient(TossPaymentsProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .build();
    }
}
