package com.campus.platform.college.repository;

import com.campus.platform.college.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.campus.platform.college.service.CollegeService;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollegeRepository extends JpaRepository<College, UUID> {
    Optional<College> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}