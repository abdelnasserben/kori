package com.kori.application.port.in.query;

import com.kori.application.query.model.MeQueryModels;
import com.kori.application.security.ActorContext;

public interface MerchantMeTxDetailQueryUseCase {
    MeQueryModels.MerchantTransactionDetails getById(ActorContext actorContext, String transactionId);
}

