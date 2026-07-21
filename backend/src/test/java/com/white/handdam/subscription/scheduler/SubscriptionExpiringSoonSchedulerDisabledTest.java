package com.white.handdam.subscription.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=test_secret_for_subscription_expiring_soon_scheduler_1234567890",
        "subscription.expiring-notification.enabled=false"
})
@ActiveProfiles("test")
class SubscriptionExpiringSoonSchedulerDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("SubscriptionExpiringSoonScheduler bean is disabled when property is false")
    void schedulerBeanIsDisabledWhenPropertyIsFalse() {
        assertThat(applicationContext.getBeansOfType(SubscriptionExpiringSoonScheduler.class)).isEmpty();
    }
}
