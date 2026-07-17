package com.white.handdam.subscription.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SubscriptionExpirationProperties.class)
public class SubscriptionSchedulingConfig {
}
