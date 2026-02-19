package com.kori.application.usecase;

import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.AgentRepositoryPort;
import com.kori.application.port.out.ClientRepositoryPort;
import com.kori.application.port.out.MerchantRepositoryPort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.client.ClientCode;
import com.kori.domain.model.merchant.MerchantCode;

import java.util.UUID;

public final class LedgerOwnerRefResolver {

    private final ClientRepositoryPort clientRepositoryPort;
    private final MerchantRepositoryPort merchantRepositoryPort;
    private final AgentRepositoryPort agentRepositoryPort;

    public LedgerOwnerRefResolver(ClientRepositoryPort clientRepositoryPort, MerchantRepositoryPort merchantRepositoryPort, AgentRepositoryPort agentRepositoryPort) {
        this.clientRepositoryPort = clientRepositoryPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
    }

    public LedgerAccountRef resolveToLedgerKey(LedgerAccountRef scope) {
        String ownerRef = scope.ownerRef().trim();

        if (isUuid(ownerRef)) return scope;

        return switch (scope.type()) {
            case CLIENT -> new LedgerAccountRef(scope.type(),
                    clientRepositoryPort.findByCode(ClientCode.of(ownerRef))
                            .orElseThrow(() -> new NotFoundException("Client not found"))
                            .id().value().toString()
            );
            case MERCHANT -> new LedgerAccountRef(scope.type(),
                    merchantRepositoryPort.findByCode(MerchantCode.of(ownerRef))
                            .orElseThrow(() -> new NotFoundException("Merchant not found"))
                            .id().value().toString()
            );
            case AGENT_WALLET, AGENT_CASH_CLEARING -> new LedgerAccountRef(scope.type(),
                    agentRepositoryPort.findByCode(AgentCode.of(ownerRef))
                            .orElseThrow(() -> new NotFoundException("Agent not found"))
                            .id().value().toString()
            );
            default -> scope; // PLATFORM_FEE_REVENUE etc: ownerRef likely already stable
        };
    }

    private boolean isUuid(String s) {
        try { UUID.fromString(s); return true; }
        catch (Exception e) { return false; }
    }
}

