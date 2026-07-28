package com.distribuidora.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI 3 / Swagger documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vgMayoristaOpenApi() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("VG Mayorista API")
                        .description("API REST para la gestión de catálogo, pedidos y distribución mayorista de VG Mayorista.")
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer customizePageable() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() != null) {
                boolean hasPageable = operation.getParameters().removeIf(p -> "pageable".equals(p.getName()));
                if (hasPageable) {
                    operation.addParametersItem(new QueryParameter()
                            .name("page")
                            .description("Número de página (0-indexed)")
                            .schema(new IntegerSchema()._default(0)));
                    operation.addParametersItem(new QueryParameter()
                            .name("size")
                            .description("Cantidad de elementos por página")
                            .schema(new IntegerSchema()._default(20)));
                    operation.addParametersItem(new QueryParameter()
                            .name("sort")
                            .description("Criterio de ordenamiento: propiedad,asc|desc (ej: id,asc o createdAt,desc)")
                            .schema(new ArraySchema().items(new StringSchema())));
                }
            }
            return operation;
        };
    }
}
