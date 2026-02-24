package com.kori.application.usecase;

import com.kori.application.command.PayByCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.exception.ValidationException;
import com.kori.application.guard.ActorStatusGuards;
import com.kori.application.guard.ActorTypeGuards;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.PayByCardResult;
import com.kori.application.security.PinFormatValidator;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.PinFailureRecorder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalUid;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PayByCardService implements PayByCardUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;

    private final TerminalRepositoryPort terminalRepositoryPort;
    private final MerchantRepositoryPort merchantRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;

    private final CardRepositoryPort cardRepositoryPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final FeePolicyPort feePolicyPort;

    private final CardSecurityPolicyPort cardSecurityPolicyPort;

    private final LedgerAppendPort ledgerAppendPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAccountLockPort ledgerAccountLockPort;

    private final AuditPort auditPort;
    private final PinHasherPort pinHasherPort;
    private final OperationAuthorizationService operationAuthorizationService;
    private final IdempotencyExecutor idempotencyExecutor;
    private final PinFailureRecorder pinFailureRecorder;

    public PayByCardService(TimeProviderPort timeProviderPort,
                            IdempotencyPort idempotencyPort,
                            IdGeneratorPort idGeneratorPort,
                            TerminalRepositoryPort terminalRepositoryPort,
                            MerchantRepositoryPort merchantRepositoryPort,
                            ClientRepositoryPort clientRepositoryPort,
                            CardRepositoryPort cardRepositoryPort,
                            TransactionRepositoryPort transactionRepositoryPort,
                            FeePolicyPort feePolicyPort,
                            CardSecurityPolicyPort cardSecurityPolicyPort,
                            LedgerAppendPort ledgerAppendPort,
                            LedgerQueryPort ledgerQueryPort, LedgerAccountLockPort ledgerAccountLockPort,
                            AuditPort auditPort,
                            PinHasherPort pinHasherPort,
                            OperationAuthorizationService operationAuthorizationService,
                            PinFailureRecorder pinFailureRecorder) {
        this.timeProviderPort = timeProviderPort;
        this.idGeneratorPort = idGeneratorPort;
        this.terminalRepositoryPort = terminalRepositoryPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.feePolicyPort = feePolicyPort;
        this.cardSecurityPolicyPort = cardSecurityPolicyPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.ledgerAccountLockPort = ledgerAccountLockPort;
        this.auditPort = auditPort;
        this.pinHasherPort = pinHasherPort;
        this.operationAuthorizationService = operationAuthorizationService;
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
        this.pinFailureRecorder = pinFailureRecorder;
    }

    @Override
    public PayByCardResult execute(PayByCardCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                PayByCardResult.class,
                () -> {

                    ActorTypeGuards.onlyTerminalCan(command.actorContext(), "initiate PayByCard");

                    String terminalUidStr = command.actorContext().actorRef();
                    TerminalUid terminalUid = TerminalUid.of(terminalUidStr);
                    Terminal terminal = terminalRepositoryPort.findByUid(terminalUid)
                            .orElseThrow(() -> new NotFoundException("Terminal not found"));
                    ActorStatusGuards.requireActiveTerminal(terminal);

                    // Card
                    Card card = cardRepositoryPort.findByCardUid(command.cardUid())
                            .orElseThrow(() -> new NotFoundException("Card not found"));
                    if (!card.isPayable()) {
                        throw new ForbiddenOperationException("Card not payable");
                    }

                    // Merchant
                    Merchant merchant = merchantRepositoryPort.findById(terminal.merchantId())
                            .orElseThrow(() -> new NotFoundException("Merchant not found"));
                    operationAuthorizationService.authorizeMerchantPayment(merchant);

                    // Client
                    Client client = clientRepositoryPort.findById(card.clientId())
                            .orElseThrow(() -> new NotFoundException("Client not found"));
                    operationAuthorizationService.authorizeClientPayment(client);

                    int maxAttempts = cardSecurityPolicyPort.maxFailedPinAttempts();
                    if (maxAttempts <= 0) {
                        throw new ValidationException(
                                "Invalid maxFailedPinAttempts policy value",
                                Map.of("maxFailedPinAttempts", maxAttempts)
                        );
                    }

                    PinFormatValidator.validate(command.pin());

                    if (!pinHasherPort.matches(command.pin(), card.hashedPin())) {
                        pinFailureRecorder.record(command.cardUid(), maxAttempts);
                        throw new ForbiddenOperationException("Invalid PIN");
                    }

                    card.onPinSuccess();
                    cardRepositoryPort.save(card);

                    // accounts reference
                    var merchantAcc = LedgerAccountRef.merchant(merchant.id().value().toString());
                    var clientAcc = LedgerAccountRef.client(card.clientId().value().toString());
                    var feeAcc = LedgerAccountRef.platformFeeRevenue();

                    // Amounts
                    Money amount = Money.positive(command.amount());
                    Money fee = feePolicyPort.cardPaymentFee(amount);
                    Money totalDebited = amount.plus(fee);

                    ledgerAccountLockPort.lock(merchantAcc);
                    Money available = ledgerQueryPort.netBalance(clientAcc);
                    if (totalDebited.isGreaterThan(available)) {
                        throw new InsufficientFundsException(
                                "Insufficient funds: need " + totalDebited + " but available " + available
                        );
                    }

                    Instant now = timeProviderPort.now();

                    TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
                    Transaction tx = Transaction.payByCard(txId, amount, now);
                    tx = transactionRepositoryPort.save(tx);

                    ledgerAppendPort.append(List.of(
                            LedgerEntry.debit(tx.id(), clientAcc, totalDebited),
                            LedgerEntry.credit(tx.id(), merchantAcc, amount),
                            LedgerEntry.credit(tx.id(), feeAcc, fee)
                    ));

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("transactionId", tx.id().value().toString());
                    metadata.put("terminalUid", terminalUidStr);
                    metadata.put("merchantCode", merchant.code().value());
                    metadata.put("cardUid", command.cardUid());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "PAY_BY_CARD",
                            command.actorContext(),
                            now,
                            metadata
                    ));

                    return new PayByCardResult(
                            tx.id().value().toString(),
                            merchant.code().value(),
                            card.cardUid(),
                            amount.asBigDecimal(),
                            fee.asBigDecimal(),
                            totalDebited.asBigDecimal()
                    );
                }
        );
    }
}
