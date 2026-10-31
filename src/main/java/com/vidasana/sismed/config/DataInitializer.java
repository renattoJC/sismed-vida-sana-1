package com.vidasana.sismed.config;

import com.vidasana.sismed.entity.Usuario;
import com.vidasana.sismed.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner inicializarUsuario(UsuarioRepository usuarioRepository) {

        return args -> {

            if (!usuarioRepository.existsByNombreUsuario("admin")) {

                Usuario administrador = new Usuario(
                        "admin",
                        passwordEncoder().encode("Admin123"),
                        "Administrador del Sistema",
                        "ADMINISTRADOR"
                );

                usuarioRepository.save(administrador);
                logger.info("Usuario administrador inicial creado correctamente.");
            }
        };
    }
}