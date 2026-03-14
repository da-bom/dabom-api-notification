package com.project.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.notification.entity.NotificationLog;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {}
