package com.project.domain.family.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.family.entity.FamilyQuota;

public interface FamilyQuotaRepository extends JpaRepository<FamilyQuota, Long> {

    Optional<FamilyQuota> findByFamilyIdAndCurrentMonth(Long familyId, LocalDate currentMonth);

    List<FamilyQuota> findByFamilyIdInAndCurrentMonth(
            Collection<Long> familyIds, LocalDate currentMonth);
}
