package com.white.handdam.subscription.repository;

import com.white.handdam.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberIdAndCreatorId(Long subscriberId, Long creatorId);
}
