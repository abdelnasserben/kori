package com.kori.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.exception.ValidationException;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.MerchantMeQueryUseCase;
import com.kori.query.port.out.MerchantMeReadPort;
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
        return readPort.findProfile(actorContext.actorRef())
                .orElseThrow(() -> new NotFoundException("Merchant not found"));
    }

    @Override
    public MeQueryModels.MeBalance getBalance(ActorContext actorContext) {
        requireMerchant(actorContext);
        return readPort.getBalance(actorContext.actorRef());
    }

    @Override
    public QueryPage<MeQueryModels.MeTransactionItem> listTransactions(ActorContext actorContext, MeQueryModels.MeTransactionsFilter filter) {
        requireMerchant(actorContext);
        return readPort.listTransactions(actorContext.actorRef(), filter);
    }

    @Override
    public QueryPage<MeQueryModels.MeTerminalItem> listTerminals(ActorContext actorContext, MeQueryModels.MeTerminalsFilter filter) {
        requireMerchant(actorContext);
        return readPort.listTerminals(actorContext.actorRef(), filter);
    }

    @Override
    public MeQueryModels.MeTerminalItem getTerminalDetails(ActorContext actorContext, String terminalUid) {
        requireMerchant(actorContext);
        validateTerminalUid(terminalUid);
        return readPort.findTerminalForMerchant(actorContext.actorRef(), terminalUid)
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
