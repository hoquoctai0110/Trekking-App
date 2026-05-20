package com.example.trekkingapp.role;

import com.example.trekkingapp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    boolean existsByUserAndRole(User user, Role role);
}
