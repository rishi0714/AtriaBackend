package com.campus.platform.college.repository;

import com.campus.platform.college.entity.CollegeDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollegeDomainRepository extends JpaRepository<CollegeDomain, UUID> {

    Optional<CollegeDomain> findByDomain(String domain);

    Optional<CollegeDomain> findByDomainAndCollege_IsActiveTrue(String domain);

    boolean existsByDomain(String domain);

    List<CollegeDomain> findAllByCollege_CollegeId(UUID collegeId);

    Optional<CollegeDomain> findByCollege_CollegeIdAndPrimaryTrue(UUID collegeId);

    @Query("SELECT cd FROM CollegeDomain cd " +
            "JOIN FETCH cd.college c " +
            "WHERE cd.domain = :domain " +
            "AND c.isActive = true")
    Optional<CollegeDomain> findByDomainAndCollege_IsActiveTrueWithCollege(
            @Param("domain") String domain);
}