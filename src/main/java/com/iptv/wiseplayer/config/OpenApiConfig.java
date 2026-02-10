package com.iptv.wiseplayer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wisePlayerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WisePlayer IPTV API")
                        .description("Comprehensive API documentation for WisePlayer IPTV Backend. " +
                                "This API handles device authentication, playlist management, " +
                                "Xtream Codes integration, and subscription services.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("WisePlayer Support")
                                .email("support@wiseplayer.com")
                                .url("https://wiseplayer.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createSecurityScheme()));
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("Enter your device token or JWT to authenticate.");
    }
}
