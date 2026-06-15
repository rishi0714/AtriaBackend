package com.campus.platform.user.repository;

import com.campus.platform.common.enums.UserRole;
import com.campus.platform.user.entity.User;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT u FROM User u WHERE u.college.collegeId = :collegeId AND u.role IN :roles")
    List<User> findAllByCollege_CollegeIdAndRoleIn(
            @Param("collegeId") UUID collegeId,
            @Param("roles") List<UserRole> roles);


    List<User> findAllByCollege_CollegeIdAndRole(UUID collegeId, UserRole role);

    long countByCollege_CollegeId(UUID collegeId);                        // ← for super admin total users

    long countByCollege_CollegeIdAndRole(UUID collegeId, UserRole role);
    // UserRepository.java
    Optional<User> findByRefreshToken(String refreshToken);

    // UserRepository
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.college WHERE u.userId = :userId")
    Optional<User> findByIdWithCollege(@Nonnull UUID userId);
}