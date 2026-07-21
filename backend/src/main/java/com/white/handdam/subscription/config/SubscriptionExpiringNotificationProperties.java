package com.white.handdam.subscription.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "subscription.expiring-notification")
public class SubscriptionExpiringNotificationProperties {

    private boolean enabled = true;
    private String cron = "0 0 9 * * *";
    private String zone = "Asia/Seoul";
    private int daysBefore = 3;
    private int batchSize = 100;
}
