package com.kori.adapters.in.rest.doc;

import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.filter.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public OpenApiCustomizer globalHeadersAndErrorsCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }

            Content errorContent = new Content()
                    .addMediaType("application/json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse")));

            components.addResponses("BadRequest",
                    new ApiResponse().description("Invalid request").content(errorContent));
            components.addResponses("Conflict",
                    new ApiResponse().description("Conflict / idempotency").content(errorContent));
            components.addResponses("UnprocessableEntity",
                    new ApiResponse().description("Business rule violation").content(errorContent));
            components.addResponses("InternalServerError",
                    new ApiResponse().description("Internal error").content(errorContent));

            Parameter actorType = new Parameter()
                    .in("header")
                    .name(RestActorContextResolver.ACTOR_TYPE_HEADER)
                    .required(true)
                    .schema(new StringSchema());
            Parameter actorId = new Parameter()
                    .in("header")
                    .name(RestActorContextResolver.ACTOR_ID_HEADER)
                    .required(true)
                    .schema(new StringSchema());

            Parameter correlationId = new Parameter()
                    .in("header")
                    .name(CorrelationIdFilter.CORRELATION_ID_HEADER)
                    .required(false)
                    .schema(new StringSchema());

            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        if (!hasParameter(operation.getParameters(), actorType.getName())) {
                            operation.addParametersItem(actorType);
                        }
                        if (!hasParameter(operation.getParameters(), actorId.getName())) {
                            operation.addParametersItem(actorId);
                        }

                        ApiResponses responses = operation.getResponses();
                        if (responses == null) {
                            responses = new ApiResponses();
                            operation.setResponses(responses);
                        }

                        // Common errors (only add if missing)
                        addResponseIfMissing(responses, "400", "#/components/responses/BadRequest");
                        addResponseIfMissing(responses, "409", "#/components/responses/Conflict");
                        addResponseIfMissing(responses, "422", "#/components/responses/UnprocessableEntity");
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
                        .name(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER)
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

    private boolean hasParameter(java.util.List<Parameter> parameters, String name) {
        if (parameters == null || name == null) {
            return false;
        }
        return parameters.stream().anyMatch(param -> name.equalsIgnoreCase(param.getName()));
    }

    private void addResponseIfMissing(ApiResponses responses, String code, String ref) {
        if (!responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse().$ref(ref));
        }
    }
}
