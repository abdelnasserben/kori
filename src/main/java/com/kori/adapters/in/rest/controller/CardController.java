package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiHeaders;
import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.doc.IdempotentOperation;
import com.kori.adapters.in.rest.dto.Requests.AddCardToExistingClientRequest;
import com.kori.adapters.in.rest.dto.Requests.AgentCardStatusRequest;
import com.kori.adapters.in.rest.dto.Requests.EnrollCardRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.AddCardToExistingClientResponse;
import com.kori.adapters.in.rest.dto.Responses.EnrollCardResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.*;
import com.kori.application.port.in.*;
import com.kori.application.security.ActorContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.CARDS)
@Tag(name = "Cards")
public class CardController {

    private final EnrollCardUseCase enrollCardUseCase;
    private final AddCardToExistingClientUseCase addCardToExistingClientUseCase;
    private final AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase;
    private final AdminUnblockCardUseCase adminUnblockCardUseCase;
    private final AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase;
    private final IdempotencyRequestHasher idempotencyRequestHasher;

    public CardController(EnrollCardUseCase enrollCardUseCase, AddCardToExistingClientUseCase addCardToExistingClientUseCase,
                          AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase,
                          AdminUnblockCardUseCase adminUnblockCardUseCase,
                          AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase, IdempotencyRequestHasher idempotencyRequestHasher) {
        this.enrollCardUseCase = enrollCardUseCase;
        this.addCardToExistingClientUseCase = addCardToExistingClientUseCase;
        this.adminUpdateCardStatusUseCase = adminUpdateCardStatusUseCase;
        this.adminUnblockCardUseCase = adminUnblockCardUseCase;
        this.agentUpdateCardStatusUseCase = agentUpdateCardStatusUseCase;
        this.idempotencyRequestHasher = idempotencyRequestHasher;
    }

    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enroll card")
    @IdempotentOperation
    public EnrollCardResponse enrollCard(
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody EnrollCardRequest request
    ) {
        var result = enrollCardUseCase.execute(
                new EnrollCardCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.phoneNumber(),
                        request.displayName(),
                        request.cardUid(),
                        request.pin()
                )
        );
        return new EnrollCardResponse(
                result.transactionId(),
                result.clientCode(),
                result.clientPhoneNumber(),
                result.cardUid(),
                result.cardPrice(),
                result.agentCommission(),
                result.clientCreated(),
                result.accountCreated()
        );
    }

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add card to existing client")
    @IdempotentOperation
    public AddCardToExistingClientResponse addCardToExistingClient(
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody AddCardToExistingClientRequest request
    ) {
        var result = addCardToExistingClientUseCase.execute(
                new AddCardToExistingClientCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.phoneNumber(),
                        request.cardUid(),
                        request.pin()
                )
        );
        return new AddCardToExistingClientResponse(
                result.transactionId(),
                result.clientId(),
                result.cardUid(),
                result.cardPrice(),
                result.agentCommission()
        );
    }

    @PatchMapping("/{cardUid}/status/admin")
    @Operation(summary = "Update card status (admin)")
    public UpdateStatusResponse adminUpdateStatus(
            @PathVariable String cardUid,
            ActorContext actorContext,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        var result = adminUpdateCardStatusUseCase.execute(
                new AdminUpdateCardStatusCommand(
                        actorContext,
                        cardUid,
                        request.targetStatus(),
                        request.reason()
                )
        );
        return new UpdateStatusResponse(result.cardUid(), result.previousStatus(), result.newStatus());
    }

    @PostMapping("/{cardUid}/unblock")
    @Operation(summary = "Unblock card")
    public UpdateStatusResponse adminUnblock(
            @PathVariable String cardUid,
            ActorContext actorContext,
            @RequestBody(required = false) UpdateStatusRequest request
    ) {
        String reason = request == null ? null : request.reason();
        var result = adminUnblockCardUseCase.execute(
                new AdminUnblockCardCommand(
                        actorContext,
                        cardUid,
                        reason
                )
        );
        return new UpdateStatusResponse(result.cardUid(), result.previousStatus(), result.newStatus());
    }

    @PatchMapping("/{cardUid}/status/agent")
    @Operation(summary = "Update card status (agent)")
    public UpdateStatusResponse agentUpdateStatus(
            @PathVariable String cardUid,
            ActorContext actorContext,
            @Valid @RequestBody AgentCardStatusRequest request
    ) {
        var result = agentUpdateCardStatusUseCase.execute(
                new AgentUpdateCardStatusCommand(
                        actorContext,
                        cardUid,
                        request.targetStatus(),
                        request.reason()
                )
        );
        return new UpdateStatusResponse(result.cardUid(), result.previousStatus(), result.newStatus());
    }
}
