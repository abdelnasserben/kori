package com.kori.application.usecase;

import com.kori.application.command.PayByCardCommand;
import com.kori.application.exception.*;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.idempotency.IdempotencyClaim;
import com.kori.application.port.out.*;
import com.kori.application.result.PayByCardResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardId;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.card.HashedPin;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class PayByCardServiceTest {

    // ======= mocks =======
    @Mock TimeProviderPort timeProviderPort;
    @Mock
    IdempotencyPort idempotencyPort;
    @Mock IdGeneratorPort idGeneratorPort;

    @Mock TerminalRepositoryPort terminalRepositoryPort;
    @Mock MerchantRepositoryPort merchantRepositoryPort;
    @Mock ClientRepositoryPort clientRepositoryPort;
    @Mock CardRepositoryPort cardRepositoryPort;

    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock FeePolicyPort feePolicyPort;

    @Mock CardSecurityPolicyPort cardSecurityPolicyPort;

    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock LedgerAccountLockPort ledgerAccountLockPort;

    @Mock AuditPort auditPort;
    @Mock PinHasherPort pinHasherPort;

    @Mock OperationStatusGuards operationStatusGuards;

    @InjectMocks PayByCardService payByCardService;

    // ======= constants (single source of truth) =======
    private static final String IDEM_KEY = "idem-1";
    private static final String REQUEST_HASH = "request-hash";
    private static final String TERMINAL_ACTOR_ID = "terminal-actor";
    private static final String CARD_UID = "CARD-001";
    private static final String GOOD_PIN = "1234";
    private static final String BAD_PIN_FORMAT = "12ab";

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final UUID TERMINAL_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String TERMINAL_UID = TERMINAL_UUID.toString();

    private static final UUID MERCHANT_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final MerchantId MERCHANT_ID = new MerchantId(MERCHANT_UUID);
    private static final MerchantCode MERCHANT_CODE = MerchantCode.of("M-123456");

    private static final UUID CLIENT_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final ClientId CLIENT_ID = new ClientId(CLIENT_UUID);

    private static final UUID TX_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final HashedPin HASHED_PIN = new HashedPin("HASHED");

    private static final Money AMOUNT = Money.of(new BigDecimal("50.00"));
    private static final Money FEE = Money.of(new BigDecimal("2.00"));
    private static final Money TOTAL_DEBITED = Money.of(new BigDecimal("52.00"));

    private static final Money AVAILABLE_ENOUGH = Money.of(new BigDecimal("999.00"));
    private static final Money AVAILABLE_NOT_ENOUGH = Money.of(new BigDecimal("10.00"));

    // ======= helpers =======
    private static ActorContext terminalActor() {
        return new ActorContext(ActorType.TERMINAL, TERMINAL_ACTOR_ID, Map.of());
    }

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, "admin-actor", Map.of());
    }

    private static PayByCardCommand cmd(String pin, BigDecimal amount) {
        return new PayByCardCommand(
                IDEM_KEY,
                REQUEST_HASH,
                terminalActor(),
                TERMINAL_UID,
                CARD_UID,
                pin,
                amount
        );
    }

    private static Terminal activeTerminal() {
        return new Terminal(new TerminalId(TERMINAL_UUID), MERCHANT_ID, Status.ACTIVE, NOW.minusSeconds(60));
    }

    private static Merchant activeMerchant() {
        return new Merchant(MERCHANT_ID, MERCHANT_CODE, Status.ACTIVE, NOW.minusSeconds(120));
    }

    private static Client activeClient() {
        return new Client(CLIENT_ID, "+2694001122", Status.ACTIVE, NOW.minusSeconds(180));
    }

    private static Card activeCardWithAttempts(int failedAttempts) {
        return new Card(
                new CardId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")),
                CLIENT_ID,
                CARD_UID,
                HASHED_PIN,
                CardStatus.ACTIVE,
                failedAttempts,
                NOW
        );
    }

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        PayByCardResult cached = new PayByCardResult(
                "tx-1",
                MERCHANT_CODE.value(),
                CARD_UID,
                new BigDecimal("50.00"),
                new BigDecimal("2.00"),
                new BigDecimal("52.00")
        );

        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.completed(cached));

        PayByCardResult out = payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal()));

        assertSame(cached, out);
        verify(idempotencyPort).claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class);

        verifyNoMoreInteractions(
                timeProviderPort,
                idGeneratorPort,
                terminalRepositoryPort,
                merchantRepositoryPort,
                clientRepositoryPort,
                cardRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                cardSecurityPolicyPort,
                ledgerAppendPort,
                ledgerQueryPort,
                auditPort,
                pinHasherPort,
                operationStatusGuards,
                idempotencyPort
        );
    }

    @Test
    void forbidden_whenActorIsNotTerminal() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());

        PayByCardCommand cmd = new PayByCardCommand(
                IDEM_KEY,
                REQUEST_HASH,
                adminActor(),
                TERMINAL_UID,
                CARD_UID,
                GOOD_PIN,
                AMOUNT.asBigDecimal()
        );

        assertThrows(ForbiddenOperationException.class, () -> payByCardService.execute(cmd));

        verify(idempotencyPort).claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class);
        verify(idempotencyPort).fail(IDEM_KEY, REQUEST_HASH);
        verifyNoMoreInteractions(idempotencyPort);

        verifyNoInteractions(
                timeProviderPort,
                idGeneratorPort,
                terminalRepositoryPort,
                merchantRepositoryPort,
                clientRepositoryPort,
                cardRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                cardSecurityPolicyPort,
                ledgerAppendPort,
                ledgerQueryPort,
                auditPort,
                pinHasherPort,
                operationStatusGuards
        );
    }

    @Test
    void throwsNotFound_whenTerminalDoesNotExist() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));
    }

    @Test
    void forbidden_whenTerminalIsNotActive() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());

        Terminal terminal = new Terminal(new TerminalId(TERMINAL_UUID), MERCHANT_ID, Status.SUSPENDED, NOW.minusSeconds(60));
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(terminal));

        assertThrows(ForbiddenOperationException.class, () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));
    }

    @Test
    void throwsNotFound_whenMerchantDoesNotExist() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));
    }

    @Test
    void forbidden_whenMerchantAccountProfileDoesNotExist_orNotActive_isHandledByGuard() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

        doThrow(new ForbiddenOperationException("MERCHANT_ACCOUNT_NOT_ACTIVE"))
                .when(operationStatusGuards).requireActiveMerchant(merchant);

        assertThrows(ForbiddenOperationException.class, () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));
    }

    @Test
    void throwsNotFound_whenCardDoesNotExist() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));
    }

    @Test
    void forbidden_whenClientNotActive_isHandledByGuard() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(activeCardWithAttempts(0)));

        Client client = activeClient();
        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));

        doThrow(new ForbiddenOperationException("CLIENT_ACCOUNT_NOT_ACTIVE"))
                .when(operationStatusGuards).requireActiveClient(client);

        assertThrows(ForbiddenOperationException.class, () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));
    }

    @Test
    void forbidden_whenCardIsNotPayable() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        Card blocked = new Card(
                new CardId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")),
                CLIENT_ID,
                CARD_UID,
                HASHED_PIN,
                CardStatus.BLOCKED,
                0,
                NOW
        );
        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(blocked));

        Client client = activeClient();
        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        doNothing().when(operationStatusGuards).requireActiveClient(client);

        assertThrows(ForbiddenOperationException.class, () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));
    }

    @Test
    void validation_whenMaxFailedPinAttemptsPolicyIsInvalid() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(activeCardWithAttempts(0)));

        Client client = activeClient();
        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        doNothing().when(operationStatusGuards).requireActiveClient(client);

        when(cardSecurityPolicyPort.maxFailedPinAttempts()).thenReturn(0);

        assertThrows(ValidationException.class, () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));
    }

    @Test
    void invalidPinFormat_throws_andDoesNotSaveCard() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(activeCardWithAttempts(0)));

        Client client = activeClient();
        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        doNothing().when(operationStatusGuards).requireActiveClient(client);

        when(cardSecurityPolicyPort.maxFailedPinAttempts()).thenReturn(3);

        assertThrows(InvalidPinFormatException.class,
                () -> payByCardService.execute(cmd(BAD_PIN_FORMAT, AMOUNT.asBigDecimal())));

        verify(cardRepositoryPort, never()).save(any(Card.class));
    }

    @Test
    void invalidPin_incrementsAttempts_savesCard_andThrowsForbidden() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        Card card = activeCardWithAttempts(0);
        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));

        Client client = activeClient();
        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        doNothing().when(operationStatusGuards).requireActiveClient(client);

        when(cardSecurityPolicyPort.maxFailedPinAttempts()).thenReturn(3);
        when(pinHasherPort.matches(GOOD_PIN, HASHED_PIN)).thenReturn(false);

        assertThrows(ForbiddenOperationException.class,
                () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));

        assertEquals(1, card.failedPinAttempts());
        assertEquals(CardStatus.ACTIVE, card.status());

        verify(cardRepositoryPort).save(card);
        verifyNoInteractions(ledgerAppendPort, transactionRepositoryPort, auditPort);
    }

    @Test
    void insufficientFunds_throws_andDoesNotCreateTransactionOrLedger() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        Card card = activeCardWithAttempts(0);
        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));

        Client client = activeClient();
        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        doNothing().when(operationStatusGuards).requireActiveClient(client);

        when(cardSecurityPolicyPort.maxFailedPinAttempts()).thenReturn(3);
        when(pinHasherPort.matches(GOOD_PIN, HASHED_PIN)).thenReturn(true);

        LedgerAccountRef clientAcc = LedgerAccountRef.client(CLIENT_UUID.toString());

        when(timeProviderPort.now()).thenReturn(NOW);
        when(feePolicyPort.cardPaymentFee(AMOUNT)).thenReturn(FEE);
        when(ledgerQueryPort.netBalance(clientAcc)).thenReturn(AVAILABLE_NOT_ENOUGH);

        assertThrows(InsufficientFundsException.class,
                () -> payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal())));

        verify(cardRepositoryPort).save(card);
        verifyNoInteractions(transactionRepositoryPort, ledgerAppendPort, auditPort);
    }

    @Test
    void happyPath_createsTx_postsLedger_publishesAudit_andSavesIdempotency() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, PayByCardResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(terminalRepositoryPort.findById(new TerminalId(TERMINAL_UUID))).thenReturn(Optional.of(activeTerminal()));

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        Card card = activeCardWithAttempts(2);
        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(card));

        Client client = activeClient();
        when(clientRepositoryPort.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        doNothing().when(operationStatusGuards).requireActiveClient(client);

        when(cardSecurityPolicyPort.maxFailedPinAttempts()).thenReturn(3);
        when(pinHasherPort.matches(GOOD_PIN, HASHED_PIN)).thenReturn(true);

        LedgerAccountRef clientAcc = LedgerAccountRef.client(CLIENT_UUID.toString());
        LedgerAccountRef merchantAcc = LedgerAccountRef.merchant(MERCHANT_UUID.toString());

        when(timeProviderPort.now()).thenReturn(NOW);

        when(feePolicyPort.cardPaymentFee(AMOUNT)).thenReturn(FEE);
        when(ledgerQueryPort.netBalance(clientAcc)).thenReturn(AVAILABLE_ENOUGH);

        when(idGeneratorPort.newUuid()).thenReturn(TX_UUID);

        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PayByCardResult out = payByCardService.execute(cmd(GOOD_PIN, AMOUNT.asBigDecimal()));

        assertEquals(TX_UUID.toString(), out.transactionId());
        assertEquals(MERCHANT_CODE.value(), out.merchantCode());
        assertEquals(CARD_UID, out.cardUid());
        assertEquals(AMOUNT.asBigDecimal(), out.amount());
        assertEquals(FEE.asBigDecimal(), out.fee());
        assertEquals(TOTAL_DEBITED.asBigDecimal(), out.totalDebited());

        assertEquals(0, card.failedPinAttempts());
        verify(cardRepositoryPort).save(card);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(ledgerCaptor.capture());
        List<LedgerEntry> entries = ledgerCaptor.getValue();
        assertEquals(3, entries.size());

        TransactionId expectedTxId = new TransactionId(TX_UUID);

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(expectedTxId)
                        && e.type() == LedgerEntryType.DEBIT
                        && e.accountRef().equals(clientAcc)
                        && e.amount().equals(TOTAL_DEBITED)
        ));

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(expectedTxId)
                        && e.type() == LedgerEntryType.CREDIT
                        && e.accountRef().equals(merchantAcc)
                        && e.amount().equals(AMOUNT)
        ));

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(expectedTxId)
                        && e.type() == LedgerEntryType.CREDIT
                        && e.accountRef().equals(LedgerAccountRef.platformFeeRevenue())
                        && e.amount().equals(FEE)
        ));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        AuditEvent event = auditCaptor.getValue();
        assertEquals("PAY_BY_CARD", event.action());
        assertEquals("TERMINAL", event.actorType());
        assertEquals(TERMINAL_ACTOR_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals(TERMINAL_UID, event.metadata().get("terminalUid"));
        assertEquals(MERCHANT_CODE.toString(), event.metadata().get("merchantCode"));
        assertEquals(TX_UUID.toString(), event.metadata().get("transactionId"));
        assertEquals(CARD_UID, event.metadata().get("cardUid"));

        verify(idempotencyPort).complete(eq(IDEM_KEY), eq(REQUEST_HASH), any(PayByCardResult.class));
    }
}
