package com.kori.application.port.out;

import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;

import java.util.Optional;

public interface AccountProfilePort {
    Optional<AccountProfile> findByAccount(LedgerAccountRef account);

    void save(AccountProfile profile);
}
