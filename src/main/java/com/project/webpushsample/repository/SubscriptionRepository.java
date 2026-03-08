package com.project.webpushsample.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.webpushsample.domain.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {}
