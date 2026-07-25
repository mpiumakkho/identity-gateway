package com.identitygateway.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationMethodRepository extends JpaRepository<VerificationMethodEntity, String> {

    List<VerificationMethodEntity> findByEnabledTrueOrderBySortOrderAscIdAsc();

    boolean existsByIdAndEnabledTrue(String id);
}
