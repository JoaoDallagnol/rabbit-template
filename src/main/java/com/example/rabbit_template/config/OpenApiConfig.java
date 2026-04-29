package com.example.rabbit_template.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rabbit Template")
                        .version("1.0.0")
                        .description("Order management API designed for future event-driven architecture with RabbitMQ (messaging not implemented yet)"));
    }
}
