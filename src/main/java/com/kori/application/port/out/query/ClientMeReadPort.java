package com.kori.application.port.out.query;

import com.kori.application.query.QueryPage;
import com.kori.application.query.model.MeQueryModels;

import java.util.List;
import java.util.Optional;

public interface ClientMeReadPort {
    Optional<MeQueryModels.MeProfile> findProfile(String clientId);

    MeQueryModels.MeBalance getBalance(String clientId);

    List<MeQueryModels.MeCardItem> listCards(String clientId);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(String clientId, MeQueryModels.MeTransactionsFilter filter);
}
