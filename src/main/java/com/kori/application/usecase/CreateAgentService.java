package com.kori.application.usecase;

import com.kori.application.command.CreateAgentCommand;
import com.kori.application.exception.ApplicationException;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.CreateAgentUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateAgentResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CreateAgentService implements CreateAgentUseCase {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;

    private final AgentRepositoryPort agentRepositoryPort;
    private final AccountProfilePort accountProfilePort;
    private final IdempotencyPort idempotencyPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public CreateAgentService(AgentRepositoryPort agentRepositoryPort, AccountProfilePort accountProfilePort, IdempotencyPort idempotencyPort, AuditPort auditPort, TimeProviderPort timeProviderPort) {
        this.agentRepositoryPort = agentRepositoryPort;
        this.accountProfilePort = accountProfilePort;
        this.idempotencyPort = idempotencyPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public CreateAgentResult execute(CreateAgentCommand command, ActorContext actorContext) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(actorContext, "actorContext");

        if (actorContext.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can create an agent.");
        }

        // Idempotency first: same key => same result, no side effects
        var cached = idempotencyPort.find(command.idempotencyKey(), CreateAgentResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        AgentCode code = generateUniqueAgentCode();
        AgentId id = AgentId.newId();
        Instant now = timeProviderPort.now();

        Agent agent = Agent.activeNew(id, code, now);
        agentRepositoryPort.save(agent);

        // Create ledger accountRef ref + profile
        LedgerAccountRef agentAccount = LedgerAccountRef.agent(id.value().toString());
        // Optional safety: avoid duplicates (should not happen in phase 1, but safe)
        accountProfilePort.findByAccount(agentAccount).ifPresent(existing -> {
            throw new ForbiddenOperationException("Agent account profile already exists for " + agentAccount);
        });

        AccountProfile profile = AccountProfile.activeNew(agentAccount, now);
        accountProfilePort.save(profile);

        CreateAgentResult result = new CreateAgentResult(id, code);
        idempotencyPort.save(command.idempotencyKey(), result);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("adminId", actorContext.actorId());
        metadata.put("agentCode", code.value());

        auditPort.publish(new AuditEvent(
                "AGENT_CREATED",
                actorContext.actorType().name(),
                actorContext.actorId(),
                now,
                metadata));

        return result;
    }

    private AgentCode generateUniqueAgentCode() {
        for (int i = 0; i < MAX_CODE_GENERATION_ATTEMPTS; i++) {
            AgentCode candidate = AgentCode.generate();
            if (!agentRepositoryPort.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ApplicationException("Unable to generate unique agentCode. Please retry.");
    }

}
