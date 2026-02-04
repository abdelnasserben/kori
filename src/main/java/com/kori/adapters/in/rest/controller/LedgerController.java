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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
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
    @Operation(summary = "Search transaction history")
    @ApiResponse(
            responseCode = "200",
            description = "Paginated transaction history",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "ledgerScope": {
                            "accountType": "MERCHANT",
                            "ownerRef": "M-000000"
                          },
                          "items": [
                            {
                              "transactionId": "trx_2001",
                              "transactionType": "CARD_PAYMENT",
                              "createdAt": "2024-08-10T14:22:10Z",
                              "clientId": "client-55",
                              "merchantId": "merchant-33",
                              "agentId": null,
                              "selfTotalDebits": 0,
                              "selfTotalCredits": 1500,
                              "selfNet": 1500,
                              "amount": 1500,
                              "fee": 30,
                              "totalDebited": 1530
                            }
                          ],
                          "nextBeforeCreatedAt": "2024-08-10T14:22:10Z",
                          "nextBeforeTransactionId": "trx_2001"
                        }
                        """)
            )
    )
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
