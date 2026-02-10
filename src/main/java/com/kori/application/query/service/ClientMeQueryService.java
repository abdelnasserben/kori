package com.kori.application.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.query.ClientMeQueryUseCase;
import com.kori.application.port.out.query.ClientMeReadPort;
import com.kori.application.query.QueryPage;
import com.kori.application.query.model.MeQueryModels;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientMeQueryService implements ClientMeQueryUseCase {

    private final ClientMeReadPort readPort;

    public ClientMeQueryService(ClientMeReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public MeQueryModels.MeProfile getProfile(ActorContext actorContext) {
        requireClient(actorContext);
        return readPort.findProfile(actorContext.actorId())
                .orElseThrow(() -> new NotFoundException("Client not found"));
    }

    @Override
    public MeQueryModels.MeBalance getBalance(ActorContext actorContext) {
        requireClient(actorContext);
        return readPort.getBalance(actorContext.actorId());
    }

    @Override
    public List<MeQueryModels.MeCardItem> listCards(ActorContext actorContext) {
        requireClient(actorContext);
        return readPort.listCards(actorContext.actorId());
    }

    @Override
    public QueryPage<MeQueryModels.MeTransactionItem> listTransactions(ActorContext actorContext, MeQueryModels.MeTransactionsFilter filter) {
        requireClient(actorContext);
        return readPort.listTransactions(actorContext.actorId(), filter);
    }

    private void requireClient(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.CLIENT) {
            throw new ForbiddenOperationException("Forbidden operation");
        }
    }
}
