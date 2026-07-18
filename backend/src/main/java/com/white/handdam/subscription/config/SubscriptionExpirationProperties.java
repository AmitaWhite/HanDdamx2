package com.white.handdam.subscription.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "subscription.expiration")
public class SubscriptionExpirationProperties {

    private boolean enabled = true;
    private String cron = "0 */10 * * * *";
    private int batchSize = 100;
}
