package com.kori.application.usecase;

import com.kori.application.command.CreateAgentCommand;
import com.kori.application.exception.ApplicationErrorCategory;
import com.kori.application.exception.ApplicationErrorCode;
import com.kori.application.exception.ApplicationException;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.idempotency.IdempotencyExecutor;
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

public final class CreateAgentService implements CreateAgentUseCase {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;

    private final AdminAccessService adminAccessService;
    private final AgentRepositoryPort agentRepositoryPort;
    private final AccountProfilePort accountProfilePort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;
    private final CodeGeneratorPort codeGeneratorPort;
    private final IdGeneratorPort idGeneratorPort;
    private final IdempotencyExecutor idempotencyExecutor;

    public CreateAgentService(AdminAccessService adminAccessService, AgentRepositoryPort agentRepositoryPort, AccountProfilePort accountProfilePort, IdempotencyPort idempotencyPort, AuditPort auditPort, TimeProviderPort timeProviderPort, CodeGeneratorPort codeGeneratorPort, IdGeneratorPort idGeneratorPort) {
        this.adminAccessService = adminAccessService;
        this.agentRepositoryPort = agentRepositoryPort;
        this.accountProfilePort = accountProfilePort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
        this.codeGeneratorPort = codeGeneratorPort;
        this.idGeneratorPort = idGeneratorPort;
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public CreateAgentResult execute(CreateAgentCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                CreateAgentResult.class,
                () -> {

                    var actorContext = command.actorContext();
                    adminAccessService.requireActiveAdmin(actorContext, "create agent");

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

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("agentCode", code.value());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "AGENT_CREATED",
                            actorContext,
                            now,
                            metadata
                    ));

                    return new CreateAgentResult(id.value().toString(), code.value());
                }
        );
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
