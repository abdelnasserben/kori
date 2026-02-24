package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.Requests.SearchLedgerRequest;
import com.kori.adapters.in.rest.dto.Responses.BalanceResponse;
import com.kori.adapters.in.rest.dto.Responses.LedgerScope;
import com.kori.adapters.in.rest.dto.Responses.TransactionHistoryItemResponse;
import com.kori.adapters.in.rest.dto.Responses.TransactionHistoryResponse;
import com.kori.application.command.GetBalanceCommand;
import com.kori.application.command.SearchTransactionHistoryCommand;
import com.kori.application.port.in.GetBalanceUseCase;
import com.kori.application.port.in.SearchTransactionHistoryUseCase;
import com.kori.application.security.ActorContext;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiPaths.LEDGER)
@Tag(name = "Ledger")
public class LedgerController {

    private final GetBalanceUseCase getBalanceUseCase;
    private final SearchTransactionHistoryUseCase searchTransactionHistoryUseCase;

    public LedgerController(GetBalanceUseCase getBalanceUseCase,
                            SearchTransactionHistoryUseCase searchTransactionHistoryUseCase) {
        this.getBalanceUseCase = getBalanceUseCase;
        this.searchTransactionHistoryUseCase = searchTransactionHistoryUseCase;
    }

    @GetMapping("/balance")
    @Operation(summary = "Get account balance")
    public BalanceResponse getBalance(
            ActorContext actorContext,
            @RequestParam LedgerAccountType accountType,
            @RequestParam @NotBlank String ownerRef
    ) {
        var result = getBalanceUseCase.execute(new GetBalanceCommand(actorContext, accountType.name(), ownerRef));
        return new BalanceResponse(result.accountType(), result.ownerRef(), result.balance());
    }

    @PostMapping("/transactions/search")
    @Operation(summary = "Search transaction history")
    public TransactionHistoryResponse searchTransactions(
            ActorContext actorContext,
            @Valid @RequestBody SearchLedgerRequest request
    ) {
        LedgerAccountRef scope = new LedgerAccountRef(request.accountType(), request.ownerRef());

        int limit = request.limit() == null ? 0 : request.limit();

        var result = searchTransactionHistoryUseCase.execute(
                new SearchTransactionHistoryCommand(
                        actorContext,
                        scope,
                        request.transactionType() == null ? null : request.transactionType().name(),
                        request.from(),
                        request.to(),
                        request.beforeCreatedAt(),
                        request.beforeTransactionId(),
                        request.minAmount(),
                        request.maxAmount(),
                        request.view(),
                        limit
                )
        );

        LedgerScope ledgerScope = new LedgerScope(
                result.ledgerAccountRef().type().name(),
                result.ledgerAccountRef().ownerRef()
        );

        var items = result.items().stream()
                .map(item -> new TransactionHistoryItemResponse(
                        item.transactionId(),
                        item.transactionType().name(),
                        item.createdAt(),
                        item.clientId(),
                        item.merchantId(),
                        item.agentId(),
                        item.selfTotalDebits(),
                        item.selfTotalCredits(),
                        item.selfNet(),
                        item.amount(),
                        item.fee(),
                        item.totalDebited()
                ))
                .collect(Collectors.toList());

        return new TransactionHistoryResponse(
                ledgerScope,
                items,
                result.nextBeforeCreatedAt(),
                result.nextBeforeTransactionId()
        );
    }
}
