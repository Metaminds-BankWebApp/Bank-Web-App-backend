package com.bank_web_app.backend.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bank_web_app.backend.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByNic(String nic);

    List<User> findAllByRole_RoleNameOrderByUpdatedAtDesc(String roleName);

    List<User> findAllByRole_RoleNameInOrderByUpdatedAtDesc(List<String> roleNames);

    List<User> findAllByRole_RoleNameInOrderByCreatedAtDesc(List<String> roleNames);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByNic(String nic);

    boolean existsByUsernameAndUserIdNot(String username, Long userId);

    boolean existsByEmailAndUserIdNot(String email, Long userId);

    boolean existsByNicAndUserIdNot(String nic, Long userId);

    long countByRole_RoleNameIn(List<String> roleNames);

    @Query(
        """
        select
            year(u.createdAt) as yearValue,
            month(u.createdAt) as monthValue,
            r.roleName as roleName,
            count(u.userId) as userCount
        from User u
        join u.role r
        where r.roleName in :roleNames
          and u.createdAt >= :startDateTime
        group by year(u.createdAt), month(u.createdAt), r.roleName
        order by year(u.createdAt) asc, month(u.createdAt) asc
        """
    )
    List<MonthlyUserGrowthRoleCountProjection> findMonthlyUserGrowthByRolesFromDate(
        @Param("roleNames") List<String> roleNames,
        @Param("startDateTime") LocalDateTime startDateTime
    );
}

