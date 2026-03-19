package com.project.domain.customer.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.customer.entity.CustomerQuota;

public interface CustomerQuotaRepository extends JpaRepository<CustomerQuota, Long> {

    List<CustomerQuota> findByFamilyId(Long familyId);

    List<CustomerQuota> findByFamilyIdIn(Collection<Long> familyIds);
}
