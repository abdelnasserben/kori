package com.kori.application.port.out;

import com.kori.domain.model.account.Account;
import com.kori.domain.model.account.AccountId;
import com.kori.domain.model.client.ClientId;

import java.util.Optional;

public interface AccountRepositoryPort {
    Optional<Account> findByClientId(ClientId clientId);

    Optional<Account> findById(AccountId accountId);

    Account save(Account account);
}
