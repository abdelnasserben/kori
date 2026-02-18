package com.kori.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.ClientMeTxDetailQueryUseCase;
import com.kori.query.port.out.ClientMeTxDetailReadPort;
import org.springframework.stereotype.Service;

@Service
public class ClientMeTxDetailQueryService implements ClientMeTxDetailQueryUseCase {

    private final ClientMeTxDetailReadPort readPort;

    public ClientMeTxDetailQueryService(ClientMeTxDetailReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public MeQueryModels.ClientTransactionDetails getByRef(ActorContext actorContext, String transactionRef) {
        requireClient(actorContext);
        return readPort.findOwnedByClient(actorContext.actorRef(), transactionRef)
                .orElseGet(() -> {
                    if (readPort.existsTransaction(transactionRef)) {
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
