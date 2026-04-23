package com.bank_web_app.backend.user.repository;

import com.bank_web_app.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByNic(String nic);

    List<User> findAllByRole_RoleNameOrderByUpdatedAtDesc(String roleName);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByNic(String nic);

    boolean existsByUsernameAndUserIdNot(String username, Long userId);

    boolean existsByEmailAndUserIdNot(String email, Long userId);

    boolean existsByNicAndUserIdNot(String nic, Long userId);
}
