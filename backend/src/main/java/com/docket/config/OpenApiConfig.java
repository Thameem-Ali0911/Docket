package com.docket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the springdoc-openapi Swagger UI with JWT Bearer authentication support.
 * Accessible at /swagger-ui.html (no JWT required to view docs).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI docketOpenApi() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Docket API")
                        .description("""
                                **Docket** — Document Intelligence Platform
                                
                                Upload invoices, contracts, and resumes for AI-powered structured field extraction,
                                plain-English summarization, and template-diff anomaly detection.
                                
                                All endpoints except auth and health require a JWT Bearer token obtained from
                                `POST /api/auth/login`. Use the **Authorize** button to set your token.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Thameem Ali")
                                .email("thameem@docket.ai"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token issued by POST /api/auth/login. Paste value without 'Bearer ' prefix.")));
    }
}
