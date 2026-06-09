package com.campus.platform.club.repository;

import com.campus.platform.club.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubRepository extends JpaRepository<Club, UUID> {

    List<Club> findAllByCollege_CollegeId(UUID collegeId);

    Optional<Club> findByClubIdAndCollege_CollegeId(UUID clubId, UUID collegeId);

    boolean existsByCollege_CollegeIdAndName(UUID collegeId, String name);

    boolean existsByManagedBy_UserId(UUID userId);

    Optional<Club> findByManagedBy_UserId(UUID userId);

    long countByCollege_CollegeId(UUID collegeId); // ← for college admin dashboard

}