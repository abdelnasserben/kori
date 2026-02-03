package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.AgentCardStatusRequest;
import com.kori.adapters.in.rest.dto.Requests.EnrollCardRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.dto.Responses.EnrollCardResponse;
import com.kori.adapters.in.rest.dto.Responses.UpdateStatusResponse;
import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.command.AdminUpdateCardStatusCommand;
import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.application.port.in.AdminUpdateCardStatusUseCase;
import com.kori.application.port.in.AgentUpdateCardStatusUseCase;
import com.kori.application.port.in.EnrollCardUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.CARDS)
public class CardController {

    private final EnrollCardUseCase enrollCardUseCase;
    private final AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase;
    private final AdminUnblockCardUseCase adminUnblockCardUseCase;
    private final AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase;

    public CardController(EnrollCardUseCase enrollCardUseCase,
                          AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase,
                          AdminUnblockCardUseCase adminUnblockCardUseCase,
                          AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase) {
        this.enrollCardUseCase = enrollCardUseCase;
        this.adminUpdateCardStatusUseCase = adminUpdateCardStatusUseCase;
        this.adminUnblockCardUseCase = adminUnblockCardUseCase;
        this.agentUpdateCardStatusUseCase = agentUpdateCardStatusUseCase;
    }

    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollCardResponse enrollCard(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody EnrollCardRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = enrollCardUseCase.execute(
                new EnrollCardCommand(
                        idempotencyKey,
                        actorContext,
                        request.phoneNumber(),
                        request.cardUid(),
                        request.pin(),
                        request.agentCode()
                )
        );
        return new EnrollCardResponse(
                result.transactionId(),
                result.clientPhoneNumber(),
                result.cardUid(),
                result.cardPrice(),
                result.agentCommission(),
                result.clientCreated(),
                result.accountCreated()
        );
    }

    @PatchMapping("/{cardUid}/status/admin")
    public UpdateStatusResponse adminUpdateStatus(
            @PathVariable String cardUid,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = adminUpdateCardStatusUseCase.execute(
                new AdminUpdateCardStatusCommand(
                        actorContext,
                        UUID.fromString(cardUid),
                        request.targetStatus(),
                        request.reason()
                )
        );
        return new UpdateStatusResponse(result.cardUid().toString(), result.previousStatus(), result.newStatus());
    }

    @PostMapping("/{cardUid}/unblock")
    public UpdateStatusResponse adminUnblock(
            @PathVariable String cardUid,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @RequestBody(required = false) UpdateStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        String reason = request == null ? null : request.reason();
        var result = adminUnblockCardUseCase.execute(
                new AdminUnblockCardCommand(
                        actorContext,
                        UUID.fromString(cardUid),
                        reason
                )
        );
        return new UpdateStatusResponse(result.cardUid().toString(), result.previousStatus(), result.newStatus());
    }

    @PatchMapping("/{cardUid}/status/agent")
    public UpdateStatusResponse agentUpdateStatus(
            @PathVariable String cardUid,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody AgentCardStatusRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = agentUpdateCardStatusUseCase.execute(
                new AgentUpdateCardStatusCommand(
                        actorContext,
                        UUID.fromString(cardUid),
                        request.agentCode(),
                        request.targetStatus(),
                        request.reason()
                )
        );
        return new UpdateStatusResponse(result.cardUid().toString(), result.previousStatus(), result.newStatus());
    }
}
