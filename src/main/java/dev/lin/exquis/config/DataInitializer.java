package dev.lin.exquis.config;

import dev.lin.exquis.role.RoleEntity;
import dev.lin.exquis.role.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByName("USER").isEmpty()) {
                roleRepository.save(new RoleEntity(null, "USER", null));
            }
            if (roleRepository.findByName("ADMIN").isEmpty()) {
                roleRepository.save(new RoleEntity(null, "ADMIN", null));
            }
        };
    }
}
