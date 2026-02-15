package com.kori.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.utils.UuidParser;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.MerchantMeTxDetailQueryUseCase;
import com.kori.query.port.out.MerchantMeTxDetailReadPort;
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

        return readPort.findOwnedByMerchant(actorContext.actorRef(), transactionId)
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
