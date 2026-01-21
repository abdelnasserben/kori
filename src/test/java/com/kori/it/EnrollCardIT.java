package com.kori.it;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.usecase.EnrollCardService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EnrollCardIT {

    @Autowired
    private EnrollCardService enrollCardService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void enrollCard_happyPath_and_idempotent() {
        // given
        String idempotencyKey = "IDEMPOTENCY_ENROLL_001";
        String agentId = "AGENT_001";           // doit exister en DB (seed Flyway)
        String phoneNumber = "+33600000001";
        String cardUid = "CARD_UID_001";
        String pin = "1234";

        ActorContext actorContext = new ActorContext(
                ActorType.AGENT,
                agentId,
                Map.of() // metadata optionnelle
        );

        EnrollCardCommand command = newEnrollCardCommand(
                idempotencyKey,
                actorContext,
                agentId,
                phoneNumber,
                cardUid,
                pin
        );

        // when (1st call)
        EnrollCardResult r1 = enrollCardService.execute(command);

        // then - basic result
        assertThat(r1).isNotNull();
        assertThat(r1.transactionId()).isNotBlank();
        assertThat(r1.clientId()).isNotBlank();
        assertThat(r1.accountId()).isNotBlank();
        assertThat(r1.cardId()).isNotBlank();
        assertThat(r1.cardPrice()).isNotNull();
        assertThat(r1.agentCommission()).isNotNull();

        UUID txId = UUID.fromString(r1.transactionId());
        UUID clientId = UUID.fromString(r1.clientId());
        UUID accountId = UUID.fromString(r1.accountId());
        UUID cardId = UUID.fromString(r1.cardId());

        // then - DB rows exist
        assertThat(count("clients", clientId)).isEqualTo(1);
        assertThat(count("accounts", accountId)).isEqualTo(1);
        assertThat(count("cards", cardId)).isEqualTo(1);
        assertThat(count("transactions", txId)).isEqualTo(1);

        // then - card_uid unique
        Integer cardUidCount = jdbc.queryForObject(
                "select count(*) from cards where card_uid = ?",
                Integer.class,
                cardUid
        );
        assertThat(cardUidCount).isEqualTo(1);

        // then - ledger append-only entries for this tx
        Integer ledgerCount = jdbc.queryForObject(
                "select count(*) from ledger_entries where transaction_id = ?",
                Integer.class,
                txId
        );
        assertThat(ledgerCount).isEqualTo(2);

        // then - audit
        Integer auditCount = jdbc.queryForObject(
                "select count(*) from audit_events where action = ? and metadata_json like ?",
                Integer.class,
                "ENROLL_CARD",
                "%" + r1.transactionId() + "%"
        );
        assertThat(auditCount).isGreaterThanOrEqualTo(1);

        // then - idempotency record exists
        Integer idemCount = jdbc.queryForObject(
                "select count(*) from idempotency_records where idempotency_key = ?",
                Integer.class,
                idempotencyKey
        );
        assertThat(idemCount).isEqualTo(1);

        // when (2nd call) => should return cached result
        EnrollCardResult r2 = enrollCardService.execute(command);

        // then - idempotence: identical result
        assertThat(r2).isEqualTo(r1);

        // and still no extra ledger lines
        Integer ledgerCountAfter = jdbc.queryForObject(
                "select count(*) from ledger_entries where transaction_id = ?",
                Integer.class,
                txId
        );
        assertThat(ledgerCountAfter).isEqualTo(2);
    }

    private int count(String table, UUID id) {
        Integer c = jdbc.queryForObject(
                "select count(*) from " + table + " where " + "id" + " = ?",
                Integer.class,
                id
        );
        return c == null ? 0 : c;
    }

    /**
     * Construit EnrollCardCommand sans dépendre de l’ordre exact du constructeur.
     * Hypothèse raisonnable: EnrollCardCommand est un record (canonique) avec des composants
     * nommés: idempotencyKey, actorContext, agentId, phoneNumber, cardUid, pin
     * (ce sont exactement les getters utilisés dans EnrollCardService)【:contentReference[oaicite:2]{index=2}
     */
    private EnrollCardCommand newEnrollCardCommand(
            String idempotencyKey,
            ActorContext actorContext,
            String agentId,
            String phoneNumber,
            String cardUid,
            String pin
    ) {
        Class<EnrollCardCommand> clazz = EnrollCardCommand.class;

        if (!clazz.isRecord()) {
            throw new IllegalStateException("EnrollCardCommand is expected to be a record for this IT helper.");
        }

        Map<String, Object> values = Map.of(
                "idempotencyKey", idempotencyKey,
                "actorContext", actorContext,
                "agentId", agentId,
                "phoneNumber", phoneNumber,
                "cardUid", cardUid,
                "pin", pin
        );

        RecordComponent[] components = clazz.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent rc = components[i];
            String name = rc.getName();
            Object v = values.get(name);
            if (v == null) {
                throw new IllegalStateException("Missing value for EnrollCardCommand component: " + name);
            }
            args[i] = v;
            paramTypes[i] = rc.getType();
        }

        try {
            Constructor<EnrollCardCommand> ctor = clazz.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build EnrollCardCommand via record canonical constructor", e);
        }
    }
}
