package com.project.domain.webpush.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.webpush.entity.PushSubscription;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByCustomerId(Long customerId);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findAllByCustomerIdIn(List<Long> customerIds);
}
