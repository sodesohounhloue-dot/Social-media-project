package fr.mosaique;

import fr.mosaique.accounts.Admin;
import fr.mosaique.accounts.User;
import fr.mosaique.accounts.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class MosaiqueApplication {
    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of("data"));
        SpringApplication.run(MosaiqueApplication.class, args);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    ApplicationRunner prepareDatabase(UserRepository users) {
        return arguments -> {
            Arrays.stream(arguments.getSourceArgs())
                .filter(argument -> argument.startsWith("--promote-admin="))
                .map(argument -> argument.substring("--promote-admin=".length()).trim().toLowerCase())
                .findFirst()
                .ifPresent(email -> promoteAdmin(users, email));
        };
    }

    private static void promoteAdmin(UserRepository users, String email) {
        User user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException("Create this account before promoting it."));
        if (user instanceof Admin) return;

        Admin admin = new Admin(user);
        users.delete(user);
        users.flush();
        users.save(admin);
    }
}