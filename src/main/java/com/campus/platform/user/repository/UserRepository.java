package com.campus.platform.user.repository;

import com.campus.platform.common.enums.UserRole;
import com.campus.platform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmail(String email);

    boolean existsByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    List<User> findAllByCollege_CollegeId(UUID collegeId);

    List<User> findAllByCollege_CollegeIdAndRole(UUID collegeId, UserRole role);

    long countByCollege_CollegeId(UUID collegeId);                        // ← for super admin total users

    long countByCollege_CollegeIdAndRole(UUID collegeId, UserRole role);
}