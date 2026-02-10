package com.kori.application.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.in.query.MerchantMeQueryUseCase;
import com.kori.application.port.out.query.MerchantMeReadPort;
import com.kori.application.query.QueryPage;
import com.kori.application.query.model.MeQueryModels;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import org.springframework.stereotype.Service;

@Service
public class MerchantMeQueryService implements MerchantMeQueryUseCase {

    private final MerchantMeReadPort readPort;

    public MerchantMeQueryService(MerchantMeReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public MeQueryModels.MeProfile getProfile(ActorContext actorContext) {
        requireMerchant(actorContext);
        return readPort.findProfile(actorContext.actorId())
                .orElseThrow(() -> new NotFoundException("Merchant not found"));
    }

    @Override
    public MeQueryModels.MeBalance getBalance(ActorContext actorContext) {
        requireMerchant(actorContext);
        return readPort.getBalance(actorContext.actorId());
    }

    @Override
    public QueryPage<MeQueryModels.MeTransactionItem> listTransactions(ActorContext actorContext, MeQueryModels.MeTransactionsFilter filter) {
        requireMerchant(actorContext);
        return readPort.listTransactions(actorContext.actorId(), filter);
    }

    @Override
    public QueryPage<MeQueryModels.MeTerminalItem> listTerminals(ActorContext actorContext, MeQueryModels.MeTerminalsFilter filter) {
        requireMerchant(actorContext);
        return readPort.listTerminals(actorContext.actorId(), filter);
    }

    @Override
    public MeQueryModels.MeTerminalItem getTerminalDetails(ActorContext actorContext, String terminalUid) {
        requireMerchant(actorContext);
        validateTerminalUid(terminalUid);
        return readPort.findTerminalForMerchant(actorContext.actorId(), terminalUid)
                .orElseGet(() -> {
                    if (readPort.existsTerminal(terminalUid)) {
                        throw new ForbiddenOperationException("Forbidden operation");
                    }
                    throw new NotFoundException("Terminal not found");
                });
    }

    private void validateTerminalUid(String terminalUid) {
        try {
            java.util.UUID.fromString(terminalUid);
        } catch (Exception ex) {
            throw new ValidationException("Invalid terminalUid", java.util.Map.of("field", "terminalUid"));
        }
    }

    private void requireMerchant(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.MERCHANT) {
            throw new ForbiddenOperationException("Forbidden operation");
        }
    }
}
