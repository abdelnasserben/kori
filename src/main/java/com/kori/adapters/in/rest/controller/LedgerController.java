package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.RestActorContextResolver;
import com.kori.adapters.in.rest.dto.Requests.SearchLedgerRequest;
import com.kori.adapters.in.rest.dto.Responses.BalanceResponse;
import com.kori.adapters.in.rest.dto.Responses.LedgerScope;
import com.kori.adapters.in.rest.dto.Responses.TransactionHistoryItemResponse;
import com.kori.adapters.in.rest.dto.Responses.TransactionHistoryResponse;
import com.kori.application.command.GetBalanceCommand;
import com.kori.application.command.SearchTransactionHistoryCommand;
import com.kori.application.command.TransactionHistoryView;
import com.kori.application.port.in.GetBalanceUseCase;
import com.kori.application.port.in.SearchTransactionHistoryUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiPaths.LEDGER)
public class LedgerController {

    private final GetBalanceUseCase getBalanceUseCase;
    private final SearchTransactionHistoryUseCase searchTransactionHistoryUseCase;

    public LedgerController(GetBalanceUseCase getBalanceUseCase,
                            SearchTransactionHistoryUseCase searchTransactionHistoryUseCase) {
        this.getBalanceUseCase = getBalanceUseCase;
        this.searchTransactionHistoryUseCase = searchTransactionHistoryUseCase;
    }

    @GetMapping("/balance")
    public BalanceResponse getBalance(
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String ownerRef
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        var result = getBalanceUseCase.execute(new GetBalanceCommand(actorContext, accountType, ownerRef));
        return new BalanceResponse(result.accountType(), result.ownerRef(), result.balance());
    }

    @PostMapping("/transactions/search")
    public TransactionHistoryResponse searchTransactions(
            @RequestHeader(RestActorContextResolver.ACTOR_TYPE_HEADER) String actorType,
            @RequestHeader(RestActorContextResolver.ACTOR_ID_HEADER) String actorId,
            @Valid @RequestBody SearchLedgerRequest request
    ) {
        var actorContext = RestActorContextResolver.resolve(actorType, actorId);
        LedgerAccountRef scope = null;
        if (request.accountType() != null && request.ownerRef() != null) {
            LedgerAccountType type = LedgerAccountType.valueOf(request.accountType().toUpperCase(Locale.ROOT));
            scope = new LedgerAccountRef(type, request.ownerRef());
        }

        TransactionHistoryView view = null;
        if (request.view() != null) {
            view = TransactionHistoryView.valueOf(request.view().toUpperCase(Locale.ROOT));
        }

        int limit = request.limit() == null ? 0 : request.limit();

        var result = searchTransactionHistoryUseCase.execute(
                new SearchTransactionHistoryCommand(
                        actorContext,
                        scope,
                        request.transactionType(),
                        request.from(),
                        request.to(),
                        request.beforeCreatedAt(),
                        request.beforeTransactionId(),
                        request.minAmount(),
                        request.maxAmount(),
                        view,
                        limit
                )
        );

        LedgerScope ledgerScope = null;
        if (result.ledgerAccountRef() != null) {
            ledgerScope = new LedgerScope(
                    result.ledgerAccountRef().type().name(),
                    result.ledgerAccountRef().ownerRef()
            );
        }

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
