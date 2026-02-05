package com.kori.it;

import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.application.port.out.*;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.admin.AdminId;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardId;
import com.kori.domain.model.card.CardStatus;
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
import com.kori.domain.model.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    protected static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    @Autowired protected JdbcTemplate jdbcTemplate;
    @Autowired protected MerchantRepositoryPort merchantRepositoryPort;
    @Autowired protected TerminalRepositoryPort terminalRepositoryPort;
    @Autowired protected ClientRepositoryPort clientRepositoryPort;
    @Autowired protected CardRepositoryPort cardRepositoryPort;
    @Autowired protected AgentRepositoryPort agentRepositoryPort;
    @Autowired protected AdminRepositoryPort adminRepositoryPort;
    @Autowired protected TransactionRepositoryPort transactionRepositoryPort;
    @Autowired protected LedgerAppendPort ledgerAppendPort;
    @Autowired protected LedgerQueryPort ledgerQueryPort;
    @Autowired protected AccountProfilePort accountProfilePort;
    @Autowired protected PayoutRepositoryPort payoutRepositoryPort;
    @Autowired protected AuditEventJpaRepository auditEventJpaRepository;
    @Autowired protected PinHasherPort pinHasherPort;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE "
                + "ledger_entries, "
                + "transactions, "
                + "payouts, "
                + "audit_events, "
                + "idempotency_records, "
                + "cards, "
                + "clients, "
                + "terminals, "
                + "merchants, "
                + "agents, "
                + "admins, "
                + "account_profiles, "
                + "fee_config, "
                + "commission_config, "
                + "security_config, "
                + "platform_config "
                + "RESTART IDENTITY CASCADE"
        );

        seedConfigRows();
    }

    protected void seedConfigRows() {
        jdbcTemplate.update(
                "INSERT INTO fee_config (id, card_enrollment_price, card_payment_fee_rate, card_payment_fee_min, "
                        + "card_payment_fee_max, merchant_withdraw_fee_rate, merchant_withdraw_fee_min, merchant_withdraw_fee_max, "
                        + "card_payment_fee_refundable, merchant_withdraw_fee_refundable, card_enrollment_price_refundable) "
                        + "VALUES (1, 10.00, 0.020000, 1.00, 5.00, 0.030000, 1.50, 6.00, false, false, false)"
        );
        jdbcTemplate.update(
                "INSERT INTO commission_config (id, card_enrollment_agent_commission, merchant_withdraw_commission_rate, "
                        + "merchant_withdraw_commission_min, merchant_withdraw_commission_max) "
                        + "VALUES (1, 3.00, 0.500000, 0.50, 2.00)"
        );
        jdbcTemplate.update(
                "INSERT INTO security_config (id, max_failed_pin_attempts) VALUES (1, 3)"
        );
        jdbcTemplate.update(
                "INSERT INTO platform_config (id, agent_cash_limit_global) VALUES (1, 0.00)"
        );
    }

    protected ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, "admin-1", Map.of());
    }

    protected ActorContext agentActor(String agentId) {
        return new ActorContext(ActorType.AGENT, agentId, Map.of());
    }

    protected ActorContext terminalActor(String terminalId) {
        return new ActorContext(ActorType.TERMINAL, terminalId, Map.of());
    }

    protected Agent createActiveAgent(String code) {
        Agent agent = Agent.activeNew(new AgentId(UUID.randomUUID()), AgentCode.of(code), NOW.minusSeconds(300));
        agentRepositoryPort.save(agent);
        createActiveAccountProfile(LedgerAccountRef.agent(agent.id().value().toString()));
        return agent;
    }

    protected Admin createActiveAdmin() {
        Admin admin = Admin.activeNew(new AdminId(UUID.randomUUID()), NOW.minusSeconds(240));
        adminRepositoryPort.save(admin);
        return admin;
    }

    protected Merchant createActiveMerchant(String code) {
        Merchant merchant = new Merchant(new MerchantId(UUID.randomUUID()), MerchantCode.of(code), Status.ACTIVE, NOW.minusSeconds(360));
        merchantRepositoryPort.save(merchant);
        createActiveAccountProfile(LedgerAccountRef.merchant(merchant.id().value().toString()));
        return merchant;
    }

    protected Terminal createActiveTerminal(Merchant merchant) {
        Terminal terminal = new Terminal(new TerminalId(UUID.randomUUID()), merchant.id(), Status.ACTIVE, NOW.minusSeconds(120));
        terminalRepositoryPort.save(terminal);
        return terminal;
    }

    protected Client createActiveClient(String phoneNumber) {
        Client client = new Client(new ClientId(UUID.randomUUID()), phoneNumber, Status.ACTIVE, NOW.minusSeconds(420));
        clientRepositoryPort.save(client);
        createActiveAccountProfile(LedgerAccountRef.client(client.id().value().toString()));
        return client;
    }

    protected Card createActiveCard(Client client, String cardUid, String pin) {
        Card card = new Card(
                new CardId(UUID.randomUUID()),
                client.id(),
                cardUid,
                pinHasherPort.hash(pin),
                CardStatus.ACTIVE,
                0,
                NOW.minusSeconds(60)
        );
        cardRepositoryPort.save(card);
        return card;
    }

    protected void createCardWithStatus(Client client, String cardUid, String pin, CardStatus status, int failedAttempts) {
        Card card = new Card(
                new CardId(UUID.randomUUID()),
                client.id(),
                cardUid,
                pinHasherPort.hash(pin),
                status,
                failedAttempts,
                NOW.minusSeconds(60)
        );
        cardRepositoryPort.save(card);
    }

    protected void createActiveAccountProfile(LedgerAccountRef accountRef) {
        AccountProfile profile = AccountProfile.activeNew(accountRef, NOW.minusSeconds(500));
        accountProfilePort.save(profile);
    }

    protected void seedLedgerCredit(LedgerAccountRef accountRef, BigDecimal amount) {
        TransactionId txId = new TransactionId(UUID.randomUUID());
        Transaction tx = new Transaction(txId, TransactionType.PAY_BY_CARD, Money.of(amount), NOW.minusSeconds(720), null);
        transactionRepositoryPort.save(tx);
        ledgerAppendPort.append(List.of(LedgerEntry.credit(tx.id(), accountRef, Money.of(amount))));
    }
}
