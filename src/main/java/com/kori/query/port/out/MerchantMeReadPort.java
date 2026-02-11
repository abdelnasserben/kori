package com.kori.query.port.out;

import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;

import java.util.Optional;

public interface MerchantMeReadPort {
    Optional<MeQueryModels.MeProfile> findProfile(String merchantId);

    MeQueryModels.MeBalance getBalance(String merchantId);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(String merchantId, MeQueryModels.MeTransactionsFilter filter);

    QueryPage<MeQueryModels.MeTerminalItem> listTerminals(String merchantId, MeQueryModels.MeTerminalsFilter filter);

    Optional<MeQueryModels.MeTerminalItem> findTerminalForMerchant(String merchantId, String terminalUid);

    boolean existsTerminal(String terminalUid);
}
