package com.deltasoft.labinventory.config;

import com.deltasoft.labinventory.domain.AppUser;
import com.deltasoft.labinventory.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
public class SeedUserLoader {

    @Bean
    CommandLineRunner seedUser(AppUserRepository users,
                               PasswordEncoder encoder,
                               @Value("${app.seed.username:yash.s}") String username,
                               @Value("${app.seed.password:labtech}") String password,
                               @Value("${app.seed.display-name:Lab Tech}") String displayName) {
        return args -> {
            String uname = username == null ? "yash.s" : username.trim().toLowerCase();
            if (uname.isEmpty()) return;
            users.findByUsernameIgnoreCase(uname).ifPresentOrElse(
                    existing -> {
                        existing.setPasswordHash(encoder.encode(password));
                        existing.setDisplayName(displayName);
                        users.save(existing);
                    },
                    () -> users.save(new AppUser(uname, encoder.encode(password), displayName, "LAB_TECH"))
            );
        };
    }
}
