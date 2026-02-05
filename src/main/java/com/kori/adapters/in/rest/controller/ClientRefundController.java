package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.doc.IdempotentOperation;
import com.kori.adapters.in.rest.dto.Requests.FailClientRefundRequest;
import com.kori.adapters.in.rest.dto.Requests.RequestClientRefundRequest;
import com.kori.adapters.in.rest.dto.Responses.ClientRefundResponse;
import com.kori.application.command.CompleteClientRefundCommand;
import com.kori.application.command.FailClientRefundCommand;
import com.kori.application.command.RequestClientRefundCommand;
import com.kori.application.port.in.CompleteClientRefundUseCase;
import com.kori.application.port.in.FailClientRefundUseCase;
import com.kori.application.port.in.RequestClientRefundUseCase;
import com.kori.application.security.ActorContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.CLIENT_REFUNDS)
@Tag(name = "Client refunds")
public class ClientRefundController {

    private final RequestClientRefundUseCase requestClientRefundUseCase;
    private final CompleteClientRefundUseCase completeClientRefundUseCase;
    private final FailClientRefundUseCase failClientRefundUseCase;
    private final IdempotencyRequestHasher idempotencyRequestHasher;

    public ClientRefundController(RequestClientRefundUseCase requestClientRefundUseCase,
                                  CompleteClientRefundUseCase completeClientRefundUseCase,
                                  FailClientRefundUseCase failClientRefundUseCase,
                                  IdempotencyRequestHasher idempotencyRequestHasher) {
        this.requestClientRefundUseCase = requestClientRefundUseCase;
        this.completeClientRefundUseCase = completeClientRefundUseCase;
        this.failClientRefundUseCase = failClientRefundUseCase;
        this.idempotencyRequestHasher = idempotencyRequestHasher;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request client refund")
    @IdempotentOperation
    public ClientRefundResponse requestClientRefund(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody RequestClientRefundRequest request
    ) {
        var result = requestClientRefundUseCase.execute(
                new RequestClientRefundCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.clientId()
                )
        );

        return new ClientRefundResponse(
                result.transactionId(),
                result.refundId(),
                result.clientId(),
                result.amount(),
                result.refundStatus()
        );
    }

    @PostMapping("/{refundId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Complete client refund")
    public void completeClientRefund(
            @PathVariable String refundId,
            ActorContext actorContext
    ) {
        completeClientRefundUseCase.execute(new CompleteClientRefundCommand(actorContext, refundId));
    }

    @PostMapping("/{refundId}/fail")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Fail client refund")
    public void failClientRefund(
            @PathVariable String refundId,
            ActorContext actorContext,
            @Valid @RequestBody FailClientRefundRequest request
    ) {
        failClientRefundUseCase.execute(new FailClientRefundCommand(actorContext, refundId, request.reason()));
    }
}
