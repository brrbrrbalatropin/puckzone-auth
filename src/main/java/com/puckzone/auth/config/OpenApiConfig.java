package com.puckzone.auth.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos del API para springdoc. Sin esquema de seguridad global: los dos
 * endpoints (register/login) son públicos — son los que EMITEN el token.
 * El server relativo "/" hace que el Try it out de Swagger UI apunte al
 * origen desde donde se cargó la spec (el gateway en Azure).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI puckzoneOpenApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/")))
                .info(new Info()
                        .title("PuckZone Auth API")
                        .version("v1")
                        .description("Registro y login con correo .edu.co colombiano. "
                                + "Emite el JWT (subject=userId UUID + claims email/username/university) "
                                + "que el resto del sistema valida localmente."));
    }
}
