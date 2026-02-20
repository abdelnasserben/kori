package com.kori.application.usecase;

import com.kori.application.command.ClientTransferCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.exception.ValidationException;
import com.kori.application.guard.ActorTypeGuards;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.ClientTransferUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.ClientTransferResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientCode;
import com.kori.domain.model.client.PhoneNumber;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientTransferService implements ClientTransferUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final FeePolicyPort feePolicyPort;
    private final PlatformConfigPort platformConfigPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAccountLockPort ledgerAccountLockPort;
    private final AuditPort auditPort;
    private final OperationAuthorizationService operationAuthorizationService;
    private final IdempotencyExecutor idempotencyExecutor;

    public ClientTransferService(TimeProviderPort timeProviderPort,
                                 IdempotencyPort idempotencyPort,
                                 IdGeneratorPort idGeneratorPort,
                                 ClientRepositoryPort clientRepositoryPort,
                                 TransactionRepositoryPort transactionRepositoryPort,
                                 FeePolicyPort feePolicyPort,
                                 PlatformConfigPort platformConfigPort,
                                 LedgerAppendPort ledgerAppendPort,
                                 LedgerQueryPort ledgerQueryPort,
                                 LedgerAccountLockPort ledgerAccountLockPort,
                                 AuditPort auditPort,
                                 OperationAuthorizationService operationAuthorizationService) {
        this.timeProviderPort = timeProviderPort;
        this.idGeneratorPort = idGeneratorPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.feePolicyPort = feePolicyPort;
        this.platformConfigPort = platformConfigPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.ledgerAccountLockPort = ledgerAccountLockPort;
        this.auditPort = auditPort;
        this.operationAuthorizationService = operationAuthorizationService;
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public ClientTransferResult execute(ClientTransferCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                ClientTransferResult.class,
                () -> {
                    ActorTypeGuards.onlyClientCan(command.actorContext(), "initiate client transfer");

                    Client sender = clientRepositoryPort.findByCode(ClientCode.of(command.actorContext().actorRef()))
                            .orElseThrow(() -> new NotFoundException("Sender client not found"));
                    operationAuthorizationService.authorizeClientPayment(sender);

                    Client recipient = clientRepositoryPort.findByPhoneNumber(PhoneNumber.of(command.recipientPhoneNumber()))
                            .orElseThrow(() -> new NotFoundException("Receiver client not found"));
                    operationAuthorizationService.authorizeClientPayment(recipient);

                    if (sender.id().equals(recipient.id())) {
                        throw new ValidationException("Client transfer to self is not allowed", Map.of("recipientPhoneNumber", command.recipientPhoneNumber()));
                    }

                    var senderAcc = LedgerAccountRef.client(sender.id().value().toString());
                    var recipientAcc = LedgerAccountRef.client(recipient.id().value().toString());
                    var feeAcc = LedgerAccountRef.platformFeeRevenue();

                    Money amount = Money.positive(command.amount());
                    Money fee = feePolicyPort.clientTransferFee(amount);
                    Money totalDebited = amount.plus(fee);

                    var platformConfig = platformConfigPort.get().orElseThrow(() -> new ForbiddenOperationException("Platform configuration is missing"));
                    Money maxPerTransaction = Money.of(platformConfig.clientTransferMaxPerTransaction());
                    if (amount.isGreaterThan(maxPerTransaction)) {
                        throw new ValidationException("Client transfer max per transaction exceeded", Map.of(
                                "amount", amount.asBigDecimal(),
                                "maxPerTransaction", maxPerTransaction.asBigDecimal()
                        ));
                    }

                    Instant now = timeProviderPort.now();
                    Instant dayStart = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
                    Instant nextDayStart = dayStart.plusSeconds(24 * 3600);
                    Money alreadyTransferredToday = ledgerQueryPort.sumDebitsByTransactionTypeAndPeriod(
                            senderAcc,
                            TransactionType.CLIENT_TRANSFER,
                            dayStart,
                            nextDayStart
                    );
                    Money projectedToday = alreadyTransferredToday.plus(amount);
                    Money dailyMax = Money.of(platformConfig.clientTransferDailyMax());
                    if (projectedToday.isGreaterThan(dailyMax)) {
                        throw new ValidationException("Client transfer daily max exceeded", Map.of(
                                "alreadyTransferredToday", alreadyTransferredToday.asBigDecimal(),
                                "amount", amount.asBigDecimal(),
                                "dailyMax", dailyMax.asBigDecimal()
                        ));
                    }

                    ledgerAccountLockPort.lock(senderAcc);
                    Money available = ledgerQueryPort.netBalance(senderAcc);
                    if (totalDebited.isGreaterThan(available)) {
                        throw new InsufficientFundsException(
                                "Insufficient funds: need " + totalDebited + " but available " + available
                        );
                    }

                    TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
                    Transaction tx = Transaction.clientTransfer(txId, amount, now);
                    tx = transactionRepositoryPort.save(tx);

                    ledgerAppendPort.append(List.of(
                            LedgerEntry.debit(tx.id(), senderAcc, totalDebited),
                            LedgerEntry.credit(tx.id(), recipientAcc, amount),
                            LedgerEntry.credit(tx.id(), feeAcc, fee)
                    ));

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("transactionId", tx.id().value().toString());
                    metadata.put("senderClientCode", sender.code().value());
                    metadata.put("recipientClientCode", recipient.code().value());
                    metadata.put("recipientPhoneNumber", recipient.phoneNumber().value());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "CLIENT_TRANSFER",
                            command.actorContext(),
                            now,
                            metadata
                    ));

                    return new ClientTransferResult(
                            tx.id().value().toString(),
                            sender.code().value(),
                            recipient.code().value(),
                            recipient.phoneNumber().value(),
                            amount.asBigDecimal(),
                            fee.asBigDecimal(),
                            totalDebited.asBigDecimal()
                    );
                }
        );
    }
}
