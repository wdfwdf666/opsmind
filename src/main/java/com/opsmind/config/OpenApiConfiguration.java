package com.opsmind.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI opsMindOpenApi() {
        return new OpenAPI().info(new Info()
                .title("OpsMind API")
                .version("v1")
                .description("Enterprise knowledge base and incident-handling RAG Agent API"));
    }
}

