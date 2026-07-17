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
        "jwt.secret=test_secret_for_subscription_scheduler_1234567890",
        "subscription.expiration.enabled=false"
})
@ActiveProfiles("test")
class SubscriptionExpirationSchedulerDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("SubscriptionExpirationScheduler bean is disabled when property is false")
    void schedulerBeanIsDisabledWhenPropertyIsFalse() {
        assertThat(applicationContext.getBeansOfType(SubscriptionExpirationScheduler.class)).isEmpty();
    }
}
