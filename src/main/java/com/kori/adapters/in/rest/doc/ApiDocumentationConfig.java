package com.kori.adapters.in.rest.doc;

import com.kori.adapters.in.rest.ApiHeaders;
import com.kori.adapters.in.rest.error.ApiErrorResponse;
import com.kori.adapters.in.rest.filter.CorrelationIdFilter;
import com.kori.application.security.ActorContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Kori API",
                description = "Executable REST contract via OpenAPI/Swagger for Kori.",
                version = "v1",
                license = @License(name = "Proprietary")
        )
)
public class ApiDocumentationConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenApiCustomizer globalHeadersAndErrorsCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }

            components.addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Bearer token for API access."));

            components.addSchemas("ApiErrorResponse", ModelConverters.getInstance()
                    .read(ApiErrorResponse.class)
                    .get("ApiErrorResponse"));

            Content errorContent = new Content()
                    .addMediaType("application/json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse")));

            components.addResponses("BadRequest",
                    new ApiResponse().description("Invalid request").content(errorContent));
            components.addResponses("Unauthorized",
                    new ApiResponse().description("Authentication required (missing/invalid/expired token).").content(errorContent));
            components.addResponses("Forbidden",
                    new ApiResponse().description("Authenticated but not authorized to perform this operation.").content(errorContent));
            components.addResponses("Conflict",
                    new ApiResponse().description("Conflict / idempotency").content(errorContent));
            components.addResponses("InternalServerError",
                    new ApiResponse().description("Internal error").content(errorContent));

            openApi.setSecurity(List.of(new SecurityRequirement().addList(SECURITY_SCHEME)));

            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) ->
                    pathItem.readOperations().forEach(operation -> {
                        if (isPublicPath(path)) {
                            operation.setSecurity(List.of());
                            return;
                        }
                        Parameter correlationId = headerParameter(
                                CorrelationIdFilter.CORRELATION_ID_HEADER,
                                false,
                                "Optional correlation identifier for request tracing."
                        );
                        if (!hasParameter(operation.getParameters(), correlationId.getName())) {
                            operation.addParametersItem(correlationId);
                        }

                        ApiResponses responses = operation.getResponses();
                        if (responses == null) {
                            responses = new ApiResponses();
                            operation.setResponses(responses);
                        }

                        // Common errors (only add if missing)
                        addResponseIfMissing(responses, "400", "#/components/responses/BadRequest");
                        addResponseIfMissing(responses, "401", "#/components/responses/Unauthorized");
                        addResponseIfMissing(responses, "403", "#/components/responses/Forbidden");
                        addResponseIfMissing(responses, "409", "#/components/responses/Conflict");
                        addResponseIfMissing(responses, "500", "#/components/responses/InternalServerError");
                    })
            );
        };
    }

    @Bean
    public OperationCustomizer idempotencyHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(IdempotentOperation.class)) { // :contentReference[oaicite:0]{index=0}
                Parameter idempotencyKey = new Parameter()
                        .in("header")
                        .name(ApiHeaders.IDEMPOTENCY_KEY)
                        .required(true)
                        .schema(new StringSchema());

                if (!hasParameter(operation.getParameters(), idempotencyKey.getName())) {
                    operation.addParametersItem(idempotencyKey);
                }
                operation.addTagsItem("Idempotency");
            }
            return operation;
        };
    }

    @Bean
    public OperationCustomizer hideActorContextParam() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            if (operation.getParameters() == null || operation.getParameters().isEmpty()) {
                return operation;
            }

            boolean methodHasActorContext = false;
            for (var p : handlerMethod.getMethodParameters()) {
                if (ActorContext.class.isAssignableFrom(p.getParameterType())) {
                    methodHasActorContext = true;
                    break;
                }
            }
            if (!methodHasActorContext) return operation;

            // Supprime le param OpenAPI généré pour ActorContext (souvent nommé "actorContext")
            operation.getParameters().removeIf(p -> "actorContext".equals(p.getName()));

            return operation;
        };
    }


    private Parameter headerParameter(String name, boolean required, String description) {
        return new Parameter()
                .in("header")
                .name(name)
                .required(required)
                .description(description)
                .schema(new StringSchema());
    }

    private boolean hasParameter(List<Parameter> parameters, String name) {
        if (parameters == null || name == null) {
            return false;
        }
        return parameters.stream().anyMatch(param -> name.equalsIgnoreCase(param.getName()));
    }

    private boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-ui.html")
                || path.startsWith("/actuator/health");
    }

    private void addResponseIfMissing(ApiResponses responses, String code, String ref) {
        if (!responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse().$ref(ref));
        }
    }
}
