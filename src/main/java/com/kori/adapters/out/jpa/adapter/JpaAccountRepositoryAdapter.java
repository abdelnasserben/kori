package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.AccountEntity;
import com.kori.adapters.out.jpa.repo.AccountJpaRepository;
import com.kori.application.port.out.AccountRepositoryPort;
import com.kori.domain.model.account.Account;
import com.kori.domain.model.account.AccountId;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaAccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository repo;

    public JpaAccountRepositoryAdapter(AccountJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findByClientId(ClientId clientId) {
        Objects.requireNonNull(clientId, "clientId must not be null");
        return repo.findByClientId(UUID.fromString(clientId.value())).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findById(AccountId accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        return repo.findById(UUID.fromString(accountId.value())).map(this::toDomain);
    }

    @Override
    @Transactional
    public Account save(Account account) {
        Objects.requireNonNull(account, "account must not be null");

        AccountEntity entity = new AccountEntity(
                UUID.fromString(account.id().value()),
                UUID.fromString(account.clientId().value()),
                account.status().name()
        );

        repo.save(entity);
        return account;
    }

    private Account toDomain(AccountEntity e) {
        return new Account(
                AccountId.of(e.getId().toString()),
                ClientId.of(e.getClientId().toString()),
                Status.valueOf(e.getStatus())
        );
    }
}
