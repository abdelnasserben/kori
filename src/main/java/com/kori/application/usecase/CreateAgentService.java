package com.kori.application.usecase;

import com.kori.application.command.CreateAgentCommand;
import com.kori.application.exception.ApplicationException;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.CreateAgentUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateAgentResult;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.audit.AuditEvent;

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
    private final CodeGeneratorPort codeGeneratorPort;
    private final IdGeneratorPort idGeneratorPort;

    public CreateAgentService(AgentRepositoryPort agentRepositoryPort, AccountProfilePort accountProfilePort, IdempotencyPort idempotencyPort, AuditPort auditPort, TimeProviderPort timeProviderPort, CodeGeneratorPort codeGeneratorPort, IdGeneratorPort idGeneratorPort) {
        this.agentRepositoryPort = agentRepositoryPort;
        this.accountProfilePort = accountProfilePort;
        this.idempotencyPort = idempotencyPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
        this.codeGeneratorPort = codeGeneratorPort;
        this.idGeneratorPort = idGeneratorPort;
    }

    @Override
    public CreateAgentResult execute(CreateAgentCommand command) {
        Objects.requireNonNull(command, "command");
        var actorContext = command.actorContext();

        if (actorContext.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can create an agent.");
        }

        // Idempotency first: same key => same result, no side effects
        var cached = idempotencyPort.find(command.idempotencyKey(), CreateAgentResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        AgentCode code = generateUniqueAgentCode();
        AgentId id = new AgentId(idGeneratorPort.newUuid());
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

        CreateAgentResult result = new CreateAgentResult(id.value().toString(), code.value());
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
            String digits = codeGeneratorPort.next6Digits();
            AgentCode candidate = AgentCode.of("A-" + digits);
            if (!agentRepositoryPort.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ApplicationException("Unable to generate unique agentCode.");
    }
}
