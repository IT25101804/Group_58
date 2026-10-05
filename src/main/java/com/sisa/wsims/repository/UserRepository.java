package com.sisa.wsims.repository;

import com.sisa.wsims.entity.AccountStatus;
import com.sisa.wsims.entity.Role;
import com.sisa.wsims.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(Role role);
    List<User> findByStatus(AccountStatus status);
    List<User> findTop5ByStatusOrderByCreatedAtDesc(AccountStatus status);
    long countByUserIdStartingWith(String prefix);
    long countByRoleInAndStatus(List<Role> roles, AccountStatus status);
}