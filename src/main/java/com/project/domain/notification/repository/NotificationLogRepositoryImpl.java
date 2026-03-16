package com.project.domain.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.project.domain.notification.entity.NotificationLog;
import com.project.domain.notification.entity.QNotificationLog;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationLogRepositoryImpl implements NotificationLogRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QNotificationLog n = QNotificationLog.notificationLog;

    @Override
    public List<NotificationLog> findByCustomerIdWithCursor(
            Long customerId,
            Long cursorId,
            int size,
            Boolean isRead,
            List<NotificationType> types,
            LocalDateTime cutoff) {

        BooleanBuilder where = new BooleanBuilder();
        where.and(n.customerId.eq(customerId));
        where.and(n.deletedAt.isNull());
        where.and(n.sentAt.goe(cutoff));

        if (types != null && !types.isEmpty()) {
            where.and(n.type.in(types));
        }
        if (isRead != null) {
            where.and(n.isRead.eq(isRead));
        }
        if (cursorId != null) {
            where.and(n.id.lt(cursorId));
        }

        return queryFactory
                .selectFrom(n)
                .where(where)
                .orderBy(n.id.desc())
                .limit(size + 1L)
                .fetch();
    }

    @Override
    public long countUnread(Long customerId, LocalDateTime cutoff) {
        Long count =
                queryFactory
                        .select(n.count())
                        .from(n)
                        .where(
                                n.customerId.eq(customerId),
                                n.isRead.isFalse(),
                                n.deletedAt.isNull(),
                                n.sentAt.goe(cutoff))
                        .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public void markAllAsRead(Long customerId, LocalDateTime cutoff) {
        queryFactory
                .update(n)
                .set(n.isRead, true)
                .where(
                        n.customerId.eq(customerId),
                        n.isRead.isFalse(),
                        n.deletedAt.isNull(),
                        n.sentAt.goe(cutoff))
                .execute();
    }
}
