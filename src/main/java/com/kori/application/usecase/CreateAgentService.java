package com.kori.application.usecase;

import com.kori.application.command.CreateAgentCommand;
import com.kori.application.exception.ApplicationErrorCategory;
import com.kori.application.exception.ApplicationErrorCode;
import com.kori.application.exception.ApplicationException;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.CreateAgentUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateAgentResult;
import com.kori.application.utils.AuditBuilder;
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

        ActorGuards.requireAdmin(actorContext, "create agent");

        // Idempotency first: same key => same result, no side effects
        var cached = idempotencyPort.find(command.idempotencyKey(), command.idempotencyRequestHash(), CreateAgentResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        AgentCode code = generateUniqueAgentCode();
        AgentId id = new AgentId(idGeneratorPort.newUuid());
        Instant now = timeProviderPort.now();

        Agent agent = Agent.activeNew(id, code, now);
        agentRepositoryPort.save(agent);

        // Active model: create both AGENT_WALLET and AGENT_CASH_CLEARING profiles.
        LedgerAccountRef walletAccount = LedgerAccountRef.agentWallet(id.value().toString());
        LedgerAccountRef clearingAccount = LedgerAccountRef.agentCashClearing(id.value().toString());

        ensureMissing(walletAccount);
        ensureMissing(clearingAccount);

        accountProfilePort.save(AccountProfile.activeNew(walletAccount, now));
        accountProfilePort.save(AccountProfile.activeNew(clearingAccount, now));

        CreateAgentResult result = new CreateAgentResult(id.value().toString(), code.value());
        idempotencyPort.save(command.idempotencyKey(), command.idempotencyRequestHash(), result);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("adminId", actorContext.actorId());
        metadata.put("agentCode", code.value());

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "AGENT_CREATED",
                actorContext,
                now,
                metadata
        ));

        return result;
    }

    private void ensureMissing(LedgerAccountRef account) {
        accountProfilePort.findByAccount(account).ifPresent(existing -> {
            throw new ForbiddenOperationException("Agent account profile already exists for " + account);
        });
    }

    private AgentCode generateUniqueAgentCode() {
        for (int i = 0; i < MAX_CODE_GENERATION_ATTEMPTS; i++) {
            String digits = codeGeneratorPort.next6Digits();
            AgentCode candidate = AgentCode.of("A-" + digits);
            if (!agentRepositoryPort.existsByCode(candidate)) {
                return candidate;
            }
        }

        throw new ApplicationException(
                ApplicationErrorCode.TECHNICAL_FAILURE,
                ApplicationErrorCategory.TECHNICAL,
                "Unable to generate unique agentCode."
        );
    }
}
