package com.kori.application.usecase;

import com.kori.application.command.PayByCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.port.out.*;
import com.kori.application.result.PayByCardResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.account.Account;
import com.kori.domain.model.account.AccountId;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardId;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.card.HashedPin;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantId;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import com.kori.domain.model.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayByCardServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock TerminalRepositoryPort terminalRepositoryPort;
    @Mock MerchantRepositoryPort merchantRepositoryPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock FeePolicyPort feePolicyPort;
    @Mock CardSecurityPolicyPort cardSecurityPolicyPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock AuditPort auditPort;
    @Mock PinHasherPort pinHasherPort;

    private PayByCardService service;

    @BeforeEach
    void setUp() {
        service = new PayByCardService(
                timeProviderPort, idempotencyPort,
                terminalRepositoryPort, merchantRepositoryPort,
                cardRepositoryPort, accountRepositoryPort,
                transactionRepositoryPort, feePolicyPort,
                cardSecurityPolicyPort,
                ledgerAppendPort, ledgerQueryPort, auditPort, pinHasherPort
        );
    }

    @Test
    void returnsCachedResultWhenIdempotencyHit() {
        PayByCardResult cached = new PayByCardResult(
                "tx1", "m1", "c1",
                BigDecimal.valueOf(50).setScale(2),
                BigDecimal.valueOf(2).setScale(2),
                BigDecimal.valueOf(52).setScale(2)
        );

        when(idempotencyPort.find("idem-1", PayByCardResult.class)).thenReturn(Optional.of(cached));

        PayByCardCommand cmd = new PayByCardCommand(
                "idem-1",
                new ActorContext(ActorType.TERMINAL, "terminal-actor", Map.of()),
                "term-1",
                "CARD-UID-1",
                "1234",
                BigDecimal.valueOf(50)
        );

        PayByCardResult result = service.execute(cmd);

        assertSame(cached, result);
        verifyNoInteractions(terminalRepositoryPort, merchantRepositoryPort, cardRepositoryPort,
                accountRepositoryPort, transactionRepositoryPort, feePolicyPort, ledgerAppendPort, auditPort, cardSecurityPolicyPort);
    }

    @Test
    void payByCard_happyPath_appendsLedger_andAudits_andSavesIdempotency() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);

        when(idempotencyPort.find("idem-2", PayByCardResult.class)).thenReturn(Optional.empty());
        when(cardSecurityPolicyPort.maxFailedPinAttempts()).thenReturn(3);

        Terminal terminal = new Terminal(TerminalId.of("term-1"), MerchantId.of("m-1"));
        when(terminalRepositoryPort.findById("term-1")).thenReturn(Optional.of(terminal));

        Merchant merchant = new Merchant(MerchantId.of("m-1"), Status.ACTIVE);
        when(merchantRepositoryPort.findById("m-1")).thenReturn(Optional.of(merchant));

        Card card = new Card(CardId.of("card-1"), AccountId.of("acc-1"), "CARD-UID-1", new HashedPin("$2a$12$fakehash"), CardStatus.ACTIVE, 0);
        when(cardRepositoryPort.findByCardUid("CARD-UID-1")).thenReturn(Optional.of(card));

        when(pinHasherPort.matches(eq("1234"), any(HashedPin.class))).thenReturn(true);

        Account account = new Account(AccountId.of("acc-1"), ClientId.of("c-1"), Status.ACTIVE);
        when(accountRepositoryPort.findById(AccountId.of("acc-1"))).thenReturn(Optional.of(account));

        // client balance sufficient
        when(ledgerQueryPort.netBalance(com.kori.domain.ledger.LedgerAccount.CLIENT, "c-1"))
                .thenReturn(Money.of(new BigDecimal("1000.00")));

        Money fee = Money.of(BigDecimal.valueOf(2));
        when(feePolicyPort.cardPaymentFee(any(Money.class))).thenReturn(fee);

        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PayByCardCommand cmd = new PayByCardCommand(
                "idem-2",
                new ActorContext(ActorType.TERMINAL, "terminal-actor", Map.of()),
                "term-1",
                "CARD-UID-1",
                "1234",
                BigDecimal.valueOf(50)
        );

        PayByCardResult result = service.execute(cmd);

        assertNotNull(result.transactionId());
        assertEquals("m-1", result.merchantId());
        assertEquals("c-1", result.clientId());
        assertEquals(BigDecimal.valueOf(50).setScale(2), result.amount());
        assertEquals(BigDecimal.valueOf(2).setScale(2), result.fee());
        assertEquals(BigDecimal.valueOf(52).setScale(2), result.totalDebited());

        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(ledgerCaptor.capture());
        assertEquals(3, ledgerCaptor.getValue().size());

        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("PAY_BY_CARD")
                        && ev.actorType().equals("TERMINAL")
                        && ev.actorId().equals("terminal-actor")
                        && ev.occurredAt().equals(now)
                        && "term-1".equals(ev.metadata().get("terminalId"))
                        && "m-1".equals(ev.metadata().get("merchantId"))
                        && ev.metadata().containsKey("transactionId")
        ));

        verify(idempotencyPort).save("idem-2", result);
    }

    @Test
    void payByCard_blocksCardAfterTooManyInvalidPinAttempts() {
        when(idempotencyPort.find("idem-x", PayByCardResult.class)).thenReturn(Optional.empty());
        when(cardSecurityPolicyPort.maxFailedPinAttempts()).thenReturn(3);

        Terminal terminal = new Terminal(TerminalId.of("term-1"), MerchantId.of("m-1"));
        when(terminalRepositoryPort.findById("term-1")).thenReturn(Optional.of(terminal));

        Merchant merchant = new Merchant(MerchantId.of("m-1"), Status.ACTIVE);
        when(merchantRepositoryPort.findById("m-1")).thenReturn(Optional.of(merchant));

        // Start at 2 failures already -> next invalid should block (threshold 3)
        Card card = new Card(CardId.of("card-1"), AccountId.of("acc-1"), "CARD-UID-1", new HashedPin("1234"), CardStatus.ACTIVE, 2);
        when(cardRepositoryPort.findByCardUid("CARD-UID-1")).thenReturn(Optional.of(card));

        PayByCardCommand cmd = new PayByCardCommand(
                "idem-x",
                new ActorContext(ActorType.TERMINAL, "terminal-actor", Map.of()),
                "term-1",
                "CARD-UID-1",
                "9999",
                BigDecimal.valueOf(50)
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepositoryPort).save(cardCaptor.capture());
        Card saved = cardCaptor.getValue();

        assertEquals(3, saved.failedPinAttempts());
        assertEquals(CardStatus.BLOCKED, saved.status());
    }

    @Test
    void payByCard_forbidden_whenActorNotTerminal() {
        when(idempotencyPort.find("idem-3", PayByCardResult.class)).thenReturn(Optional.empty());

        PayByCardCommand cmd = new PayByCardCommand(
                "idem-3",
                new ActorContext(ActorType.ADMIN, "admin-1", Map.of()),
                "term-1",
                "CARD-UID-1",
                "1234",
                BigDecimal.valueOf(50)
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }

    @Test
    void payByCard_forbidden_whenTerminalMissing() {
        when(idempotencyPort.find("idem-4", PayByCardResult.class)).thenReturn(Optional.empty());
        when(terminalRepositoryPort.findById("term-404")).thenReturn(Optional.empty());

        PayByCardCommand cmd = new PayByCardCommand(
                "idem-4",
                new ActorContext(ActorType.TERMINAL, "terminal-actor", Map.of()),
                "term-404",
                "CARD-UID-1",
                "1234",
                BigDecimal.valueOf(50)
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
        verifyNoInteractions(merchantRepositoryPort, cardRepositoryPort, accountRepositoryPort,
                transactionRepositoryPort, feePolicyPort, ledgerAppendPort, auditPort);
    }

    @Test
    void payByCard_fails_whenInsufficientFunds() {
        when(idempotencyPort.find("idem-insufficient", PayByCardResult.class)).thenReturn(Optional.empty());
        when(cardSecurityPolicyPort.maxFailedPinAttempts()).thenReturn(3);

        Terminal terminal = new Terminal(TerminalId.of("term-1"), MerchantId.of("m-1"));
        when(terminalRepositoryPort.findById("term-1")).thenReturn(Optional.of(terminal));

        Merchant merchant = new Merchant(MerchantId.of("m-1"), Status.ACTIVE);
        when(merchantRepositoryPort.findById("m-1")).thenReturn(Optional.of(merchant));

        Card card = new Card(
                CardId.of("card-1"),
                AccountId.of("acc-1"),
                "CARD-UID-1",
                new HashedPin("$2a$12$fakehash"),
                CardStatus.ACTIVE,
                0
        );
        when(cardRepositoryPort.findByCardUid("CARD-UID-1")).thenReturn(Optional.of(card));
        when(pinHasherPort.matches(eq("1234"), any(HashedPin.class))).thenReturn(true);

        Account account = new Account(AccountId.of("acc-1"), ClientId.of("c-1"), Status.ACTIVE);
        when(accountRepositoryPort.findById(AccountId.of("acc-1"))).thenReturn(Optional.of(account));

        // amount=1000, fee=20 => totalDebited=1020
        when(feePolicyPort.cardPaymentFee(any(Money.class))).thenReturn(Money.of(new BigDecimal("20.00")));

        // client balance = 0 => insufficient
        when(ledgerQueryPort.netBalance(com.kori.domain.ledger.LedgerAccount.CLIENT, "c-1"))
                .thenReturn(Money.of(new BigDecimal("0.00")));

        PayByCardCommand cmd = new PayByCardCommand(
                "idem-insufficient",
                new ActorContext(ActorType.TERMINAL, "terminal-actor", Map.of()),
                "term-1",
                "CARD-UID-1",
                "1234",
                new BigDecimal("1000.00")
        );

        assertThrows(InsufficientFundsException.class,
                () -> service.execute(cmd)
        );
    }

}
