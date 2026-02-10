package com.kori.application.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.query.ClientMeTxDetailQueryUseCase;
import com.kori.application.port.out.query.ClientMeTxDetailReadPort;
import com.kori.application.query.model.MeQueryModels;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.utils.UuidParser;
import org.springframework.stereotype.Service;

@Service
public class ClientMeTxDetailQueryService implements ClientMeTxDetailQueryUseCase {

    private final ClientMeTxDetailReadPort readPort;

    public ClientMeTxDetailQueryService(ClientMeTxDetailReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public MeQueryModels.ClientTransactionDetails getById(ActorContext actorContext, String transactionId) {
        requireClient(actorContext);
        UuidParser.parse(transactionId, "transactionId");

        return readPort.findOwnedByClient(actorContext.actorId(), transactionId)
                .orElseGet(() -> {
                    if (readPort.existsTransaction(transactionId)) {
                        throw new ForbiddenOperationException("Forbidden operation");
                    }
                    throw new NotFoundException("Transaction not found");
                });
    }

    private void requireClient(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.CLIENT) {
            throw new ForbiddenOperationException("Forbidden operation");
        }
    }
}
