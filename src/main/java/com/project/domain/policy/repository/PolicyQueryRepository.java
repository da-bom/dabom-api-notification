package com.project.domain.policy.repository;

import static com.project.domain.family.entity.QFamilyMember.familyMember;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PolicyQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<Long> findFamilyIdByTargetCustomerId(Long customerId) {
        Long familyId =
                queryFactory
                        .select(familyMember.familyId)
                        .from(familyMember)
                        .where(
                                familyMember
                                        .customerId
                                        .eq(customerId)
                                        .and(familyMember.deletedAt.isNull()))
                        .fetchOne();
        return Optional.ofNullable(familyId);
    }
}
