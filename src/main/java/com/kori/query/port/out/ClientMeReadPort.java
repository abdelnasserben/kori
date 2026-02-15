package com.kori.query.port.out;

import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;

import java.util.List;
import java.util.Optional;

public interface ClientMeReadPort {
    Optional<MeQueryModels.MeProfile> findProfile(String clientCode);

    MeQueryModels.MeBalance getBalance(String clientCode);

    List<MeQueryModels.MeCardItem> listCards(String clientCode);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(String clientCode, MeQueryModels.MeTransactionsFilter filter);
}
