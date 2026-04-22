package com.bank_web_app.backend.user.repository;

import com.bank_web_app.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    List<User> findAllByRole_RoleNameOrderByUpdatedAtDesc(String roleName);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByNic(String nic);
}
