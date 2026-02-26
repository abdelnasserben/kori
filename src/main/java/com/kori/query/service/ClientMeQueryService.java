package com.kori.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.ClientMeQueryUseCase;
import com.kori.query.port.out.ClientMeReadPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientMeQueryService implements ClientMeQueryUseCase {

    private final ClientMeReadPort readPort;

    public ClientMeQueryService(ClientMeReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public MeQueryModels.ClientProfile getProfile(ActorContext actorContext) {
        requireClient(actorContext);
        return readPort.findProfile(actorContext.actorRef())
                .orElseThrow(() -> new NotFoundException("Client not found"));
    }

    @Override
    public MeQueryModels.ActorBalance getBalance(ActorContext actorContext) {
        requireClient(actorContext);
        return readPort.getBalance(actorContext.actorRef());
    }

    @Override
    public List<MeQueryModels.MeCardItem> listCards(ActorContext actorContext) {
        requireClient(actorContext);
        return readPort.listCards(actorContext.actorRef());
    }

    @Override
    public QueryPage<MeQueryModels.MeTransactionItem> listTransactions(ActorContext actorContext, MeQueryModels.MeTransactionsFilter filter) {
        requireClient(actorContext);
        return readPort.listTransactions(actorContext.actorRef(), filter);
    }

    private void requireClient(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.CLIENT) {
            throw new ForbiddenOperationException("Forbidden operation");
        }
    }
}
