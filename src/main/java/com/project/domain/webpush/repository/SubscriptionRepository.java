package com.project.domain.webpush.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.webpush.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByCustomerId(Long customerId);

    List<Subscription> findAllByCustomerIdIn(List<Long> customerIds);
}
