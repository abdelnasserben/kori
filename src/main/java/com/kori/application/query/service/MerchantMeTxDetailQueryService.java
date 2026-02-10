package com.kori.application.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.query.MerchantMeTxDetailQueryUseCase;
import com.kori.application.port.out.query.MerchantMeTxDetailReadPort;
import com.kori.application.query.model.MeQueryModels;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.utils.UuidParser;
import org.springframework.stereotype.Service;

@Service
public class MerchantMeTxDetailQueryService implements MerchantMeTxDetailQueryUseCase {

    private final MerchantMeTxDetailReadPort readPort;

    public MerchantMeTxDetailQueryService(MerchantMeTxDetailReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public MeQueryModels.MerchantTransactionDetails getById(ActorContext actorContext, String transactionId) {
        requireMerchant(actorContext);
        UuidParser.parse(transactionId, "transactionId");

        return readPort.findOwnedByMerchant(actorContext.actorId(), transactionId)
                .orElseGet(() -> {
                    if (readPort.existsTransaction(transactionId)) {
                        throw new ForbiddenOperationException("Forbidden operation");
                    }
                    throw new NotFoundException("Transaction not found");
                });
    }

    private void requireMerchant(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.MERCHANT) {
            throw new ForbiddenOperationException("Forbidden operation");
        }
    }
}
