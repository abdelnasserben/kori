package com.kori.query.port.out;

import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;

import java.util.Optional;

public interface MerchantMeReadPort {
    Optional<MeQueryModels.MerchantProfile> findProfile(String merchantCode);

    MeQueryModels.ActorBalance getBalance(String merchantCode);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(String merchantCode, MeQueryModels.MeTransactionsFilter filter);

    QueryPage<MeQueryModels.MeTerminalItem> listTerminals(String merchantCode, MeQueryModels.MeTerminalsFilter filter);

    Optional<MeQueryModels.MeTerminalItem> findTerminalForMerchant(String merchantCode, String terminalUid);

    boolean existsTerminal(String terminalUid);
}
