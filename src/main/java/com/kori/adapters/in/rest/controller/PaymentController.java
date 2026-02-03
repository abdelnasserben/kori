package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.MerchantWithdrawAtAgentRequest;
import com.kori.adapters.in.rest.dto.Requests.PayByCardRequest;
import com.kori.adapters.in.rest.dto.Requests.ReversalRequest;
import com.kori.adapters.in.rest.dto.Responses.MerchantWithdrawAtAgentResponse;
import com.kori.adapters.in.rest.dto.Responses.PayByCardResponse;
import com.kori.adapters.in.rest.dto.Responses.ReversalResponse;
import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.command.ReversalCommand;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.port.in.ReversalUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PayByCardUseCase payByCardUseCase;
    private final MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;
    private final ReversalUseCase reversalUseCase;

    public PaymentController(PayByCardUseCase payByCardUseCase,
                             MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase,
                             ReversalUseCase reversalUseCase) {
        this.payByCardUseCase = payByCardUseCase;
        this.merchantWithdrawAtAgentUseCase = merchantWithdrawAtAgentUseCase;
        this.reversalUseCase = reversalUseCase;
    }

    @PostMapping("/card")
    @ResponseStatus(HttpStatus.CREATED)
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

    @PostMapping("/reversals")
    @ResponseStatus(HttpStatus.CREATED)
    public ReversalResponse reversal(
            @RequestHeader(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody ReversalRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = reversalUseCase.execute(
                new ReversalCommand(idempotencyKey, actorContext, request.originalTransactionId())
        );
        return new ReversalResponse(result.reversalTransactionId(), result.originalTransactionId());
    }
}
