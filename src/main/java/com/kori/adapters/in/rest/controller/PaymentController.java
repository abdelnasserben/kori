package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiHeaders;
import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.doc.IdempotentOperation;
import com.kori.adapters.in.rest.dto.Requests.*;
import com.kori.adapters.in.rest.dto.Responses.*;
import com.kori.application.command.*;
import com.kori.application.port.in.*;
import com.kori.application.security.ActorContext;
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
    private final AgentBankDepositReceiptUseCase agentBankDepositReceiptUseCase;
    private final ReversalUseCase reversalUseCase;
    private final IdempotencyRequestHasher idempotencyRequestHasher;

    public PaymentController(PayByCardUseCase payByCardUseCase,
                             MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase, CashInByAgentUseCase cashInByAgentUseCase, AgentBankDepositReceiptUseCase agentBankDepositReceiptUseCase,
                             ReversalUseCase reversalUseCase, IdempotencyRequestHasher idempotencyRequestHasher) {
        this.payByCardUseCase = payByCardUseCase;
        this.merchantWithdrawAtAgentUseCase = merchantWithdrawAtAgentUseCase;
        this.cashInByAgentUseCase = cashInByAgentUseCase;
        this.agentBankDepositReceiptUseCase = agentBankDepositReceiptUseCase;
        this.reversalUseCase = reversalUseCase;
        this.idempotencyRequestHasher = idempotencyRequestHasher;
    }

    @PostMapping("/card")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pay by card")
    @IdempotentOperation
    public PayByCardResponse payByCard(
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody PayByCardRequest request
    ) {
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
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody MerchantWithdrawAtAgentRequest request
    ) {
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
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody CashInByAgentRequest request
    ) {
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

    @PostMapping("/agent-bank-deposits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Admin records agent bank deposit receipt")
    @IdempotentOperation
    public AgentBankDepositReceiptResponse agentBankDepositReceipt(
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody AgentBankDepositReceiptRequest request
    ) {
        var result = agentBankDepositReceiptUseCase.execute(
                new AgentBankDepositReceiptCommand(
                        idempotencyKey,
                        idempotencyRequestHasher.hashPayload(request),
                        actorContext,
                        request.agentCode(),
                        request.amount()
                )
        );
        return new AgentBankDepositReceiptResponse(
                result.transactionId(),
                result.agentCode(),
                result.amount()
        );
    }

    @PostMapping("/reversals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reverse transaction")
    @IdempotentOperation
    public ReversalResponse reversal(
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            ActorContext actorContext,
            @Valid @RequestBody ReversalRequest request
    ) {
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
