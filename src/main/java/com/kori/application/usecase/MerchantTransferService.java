package com.kori.application.usecase;

import com.kori.application.command.MerchantTransferCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.exception.ValidationException;
import com.kori.application.guard.ActorTypeGuards;
import com.kori.application.guard.TransactionAmountLimitGuard;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.MerchantTransferUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.MerchantTransferResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MerchantTransferService implements MerchantTransferUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final MerchantRepositoryPort merchantRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final FeePolicyPort feePolicyPort;
    private final PlatformConfigPort platformConfigPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAccountLockPort ledgerAccountLockPort;
    private final AuditPort auditPort;
    private final OperationAuthorizationService operationAuthorizationService;
    private final IdempotencyExecutor idempotencyExecutor;

    public MerchantTransferService(TimeProviderPort timeProviderPort,
                                   IdempotencyPort idempotencyPort,
                                   IdGeneratorPort idGeneratorPort,
                                   MerchantRepositoryPort merchantRepositoryPort,
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
        this.merchantRepositoryPort = merchantRepositoryPort;
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
    public MerchantTransferResult execute(MerchantTransferCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                MerchantTransferResult.class,
                () -> {
                    ActorTypeGuards.onlyMerchantCan(command.actorContext(), "initiate merchant transfer");

                    Merchant sender = merchantRepositoryPort.findByCode(MerchantCode.of(command.actorContext().actorRef()))
                            .orElseThrow(() -> new NotFoundException("Sender merchant not found"));
                    operationAuthorizationService.authorizeMerchantPayment(sender);

                    Merchant recipient = merchantRepositoryPort.findByCode(MerchantCode.of(command.recipientMerchantCode()))
                            .orElseThrow(() -> new NotFoundException("Recipient merchant not found"));
                    operationAuthorizationService.authorizeMerchantPayment(recipient);

                    if (sender.id().equals(recipient.id())) {
                        throw new ValidationException("Merchant transfer to self is not allowed", Map.of("recipientMerchantCode", command.recipientMerchantCode()));
                    }

                    var senderAcc = LedgerAccountRef.merchant(sender.id().value().toString());
                    var recipientAcc = LedgerAccountRef.merchant(recipient.id().value().toString());
                    var feeAcc = LedgerAccountRef.platformFeeRevenue();

                    Money amount = Money.positive(command.amount());
                    Money fee = feePolicyPort.merchantTransferFee(amount);
                    Money totalDebited = amount.plus(fee);

                    var platformConfig = platformConfigPort.get().orElseThrow(() -> new ForbiddenOperationException("Platform configuration is missing"));
                    Money minPerTransaction = Money.of(platformConfig.merchantTransferMinPerTransaction());
                    TransactionAmountLimitGuard.ensureMinPerTransaction(amount, minPerTransaction, "MERCHANT_TRANSFER");

                    Money maxPerTransaction = Money.of(platformConfig.merchantTransferMaxPerTransaction());
                    TransactionAmountLimitGuard.ensureMaxPerTransaction(amount, maxPerTransaction, "MERCHANT_TRANSFER");

                    Instant now = timeProviderPort.now();
                    Instant dayStart = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
                    Instant nextDayStart = dayStart.plusSeconds(24 * 3600);
                    Money alreadyTransferredToday = ledgerQueryPort.sumDebitsByTransactionTypeAndPeriod(
                            senderAcc,
                            TransactionType.MERCHANT_TRANSFER,
                            dayStart,
                            nextDayStart
                    );
                    Money projectedToday = alreadyTransferredToday.plus(amount);
                    Money dailyMax = Money.of(platformConfig.merchantTransferDailyMax());
                    if (projectedToday.isGreaterThan(dailyMax)) {
                        throw new ValidationException("Merchant transfer daily max exceeded", Map.of(
                                "alreadyTransferredToday", alreadyTransferredToday.asBigDecimal(),
                                "amount", amount.asBigDecimal(),
                                "dailyMax", dailyMax.asBigDecimal()
                        ));
                    }

                    ledgerAccountLockPort.lock(senderAcc);
                    Money available = ledgerQueryPort.netBalance(senderAcc);
                    if (totalDebited.isGreaterThan(available)) {
                        throw new InsufficientFundsException(
                                "Insufficient merchant funds: need " + totalDebited + " but available " + available
                        );
                    }

                    TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
                    Transaction tx = Transaction.merchantTransfer(txId, amount, now);
                    tx = transactionRepositoryPort.save(tx);

                    ledgerAppendPort.append(List.of(
                            LedgerEntry.debit(tx.id(), senderAcc, totalDebited),
                            LedgerEntry.credit(tx.id(), recipientAcc, amount),
                            LedgerEntry.credit(tx.id(), feeAcc, fee)
                    ));

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("transactionId", tx.id().value().toString());
                    metadata.put("senderMerchantCode", sender.code().value());
                    metadata.put("recipientMerchantCode", recipient.code().value());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "MERCHANT_TRANSFER",
                            command.actorContext(),
                            now,
                            metadata
                    ));

                    return new MerchantTransferResult(
                            tx.id().value().toString(),
                            sender.code().value(),
                            recipient.code().value(),
                            amount.asBigDecimal(),
                            fee.asBigDecimal(),
                            totalDebited.asBigDecimal()
                    );
                }
        );
    }
}
