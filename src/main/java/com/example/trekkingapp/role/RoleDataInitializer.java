package com.example.trekkingapp.role;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleDataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        List.of("TREKKER", "TOUR_PROVIDER", "ADMIN").forEach(this::createRoleIfMissing);
    }

    private void createRoleIfMissing(String roleName) {
        roleRepository.findByRoleName(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setRoleName(roleName);
            return roleRepository.save(role);
        });
    }
}
