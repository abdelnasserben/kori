package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.doc.IdempotentOperation;
import com.kori.adapters.in.rest.dto.Requests.CashInByAgentRequest;
import com.kori.adapters.in.rest.dto.Requests.MerchantWithdrawAtAgentRequest;
import com.kori.adapters.in.rest.dto.Requests.PayByCardRequest;
import com.kori.adapters.in.rest.dto.Requests.ReversalRequest;
import com.kori.adapters.in.rest.dto.Responses.CashInByAgentResponse;
import com.kori.adapters.in.rest.dto.Responses.MerchantWithdrawAtAgentResponse;
import com.kori.adapters.in.rest.dto.Responses.PayByCardResponse;
import com.kori.adapters.in.rest.dto.Responses.ReversalResponse;
import com.kori.application.command.CashInByAgentCommand;
import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.command.ReversalCommand;
import com.kori.application.port.in.CashInByAgentUseCase;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.port.in.ReversalUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.PAYMENTS)
@Tag(name = "Payments")
public class PaymentController {

    private final PayByCardUseCase payByCardUseCase;
    private final MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;
    private final CashInByAgentUseCase cashInByAgentUseCase;
    private final ReversalUseCase reversalUseCase;
    private final IdempotencyRequestHasher idempotencyRequestHasher;

    public PaymentController(PayByCardUseCase payByCardUseCase,
                             MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase, CashInByAgentUseCase cashInByAgentUseCase,
                             ReversalUseCase reversalUseCase, IdempotencyRequestHasher idempotencyRequestHasher) {
        this.payByCardUseCase = payByCardUseCase;
        this.merchantWithdrawAtAgentUseCase = merchantWithdrawAtAgentUseCase;
        this.cashInByAgentUseCase = cashInByAgentUseCase;
        this.reversalUseCase = reversalUseCase;
        this.idempotencyRequestHasher = idempotencyRequestHasher;
    }

    @PostMapping("/card")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pay by card")
    @IdempotentOperation
    public PayByCardResponse payByCard(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody PayByCardRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = payByCardUseCase.execute(
                new PayByCardCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.terminalUid(),
                        request.cardUid(),
                        request.pin(),
                        request.amount()
                )
        );
        return new PayByCardResponse(
                result.transactionId(),
                result.merchantCode(),
                result.cardUid(),
                result.amount(),
                result.fee(),
                result.totalDebited()
        );
    }

    @PostMapping("/merchant-withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Merchant withdrawal")
    @IdempotentOperation
    public MerchantWithdrawAtAgentResponse merchantWithdrawAtAgent(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody MerchantWithdrawAtAgentRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = merchantWithdrawAtAgentUseCase.execute(
                new MerchantWithdrawAtAgentCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.merchantCode(),
                        request.agentCode(),
                        request.amount()
                )
        );
        return new MerchantWithdrawAtAgentResponse(
                result.transactionId(),
                result.merchantCode(),
                result.agentCode(),
                result.amount(),
                result.fee(),
                result.commission(),
                result.totalDebitedMerchant()
        );
    }

    @PostMapping("/cash-in")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cash-in by agent")
    @IdempotentOperation
    public CashInByAgentResponse cashInByAgent(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody CashInByAgentRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = cashInByAgentUseCase.execute(
                new CashInByAgentCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.phoneNumber(),
                        request.amount()
                )
        );
        return new CashInByAgentResponse(
                result.transactionId(),
                result.agentId(),
                result.clientId(),
                result.clientPhoneNumber(),
                result.amount()
        );
    }

    @PostMapping("/reversals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reverse transaction")
    @IdempotentOperation
    public ReversalResponse reversal(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody ReversalRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = reversalUseCase.execute(
                new ReversalCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.originalTransactionId()
                )
        );
        return new ReversalResponse(result.reversalTransactionId(), result.originalTransactionId());
    }
}
