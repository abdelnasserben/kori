package com.kori.integration.usecase;

import com.kori.adapters.out.jpa.entity.CardEntity;
import com.kori.adapters.out.jpa.repo.AuditEventJpaRepository;
import com.kori.adapters.out.jpa.repo.CardJpaRepository;
import com.kori.adapters.out.jpa.repo.IdempotencyJpaRepository;
import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AdminUnblockCardForbiddenWhenActorNotAdminIT {

    @Autowired EnrollCardUseCase enrollCardUseCase;
    @Autowired AdminUnblockCardUseCase adminUnblockCardUseCase;

    @Autowired CardJpaRepository cardJpaRepository;
    @Autowired AuditEventJpaRepository auditEventJpaRepository;
    @Autowired IdempotencyJpaRepository idempotencyJpaRepository;

    @Test
    void adminUnblock_isForbidden_whenActorIsNotAdmin() {
        // Given: card exists and is BLOCKED
        String agentId = "AGENT_001";
        String phoneNumber = "+269910" + (100000 + (int) (Math.random() * 899999));
        String cardUid = "CARD-" + UUID.randomUUID();

        enrollCardUseCase.execute(new EnrollCardCommand(
                "it-enroll-for-unblock-forbidden-" + UUID.randomUUID(),
                new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                phoneNumber,
                cardUid,
                "1234",
                agentId
        ));

        CardEntity card = cardJpaRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new AssertionError("Card should exist"));
        card.setStatus("BLOCKED");
        cardJpaRepository.saveAndFlush(card);

        String statusBefore = card.getStatus();
        long auditBefore = auditEventJpaRepository.count();
        long idemBefore = idempotencyJpaRepository.count();

        // When / Then: AGENT tries to unblock (forbidden)
        assertThrows(ForbiddenOperationException.class, () ->
                adminUnblockCardUseCase.execute(new AdminUnblockCardCommand(
                        "it-unblock-forbidden-" + UUID.randomUUID(),
                        new ActorContext(ActorType.AGENT, "agent-actor-it", Map.of()),
                        cardUid,
                        "not allowed"
                ))
        );

        // Then: status unchanged
        CardEntity after = cardJpaRepository.findByCardUid(cardUid).orElseThrow();
        assertEquals(statusBefore, after.getStatus(), "Card status must not change");

        // And: no side effects
        assertEquals(auditBefore, auditEventJpaRepository.count(), "No audit event should be written");
        assertEquals(idemBefore, idempotencyJpaRepository.count(), "No idempotency record should be written");
    }
}
