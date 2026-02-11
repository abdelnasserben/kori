package com.kori.query.port.out;

import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;

import java.util.List;
import java.util.Optional;

public interface ClientMeReadPort {
    Optional<MeQueryModels.MeProfile> findProfile(String clientId);

    MeQueryModels.MeBalance getBalance(String clientId);

    List<MeQueryModels.MeCardItem> listCards(String clientId);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(String clientId, MeQueryModels.MeTransactionsFilter filter);
}
