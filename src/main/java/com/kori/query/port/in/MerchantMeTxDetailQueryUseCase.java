package com.kori.query.port.in;

import com.kori.application.security.ActorContext;
import com.kori.query.model.me.MeQueryModels;

public interface MerchantMeTxDetailQueryUseCase {
    MeQueryModels.MerchantTransactionDetails getById(ActorContext actorContext, String transactionId);
}

