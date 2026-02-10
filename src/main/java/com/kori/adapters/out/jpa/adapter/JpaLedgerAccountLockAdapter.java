package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.repo.AccountProfileJpaRepository;
import com.kori.application.port.out.LedgerAccountLockPort;
import com.kori.domain.ledger.LedgerAccountRef;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class JpaLedgerAccountLockAdapter implements LedgerAccountLockPort {

    private final AccountProfileJpaRepository repo;

    public JpaLedgerAccountLockAdapter(AccountProfileJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo, "repo");
    }

    @Override
    @Transactional
    public void lock(LedgerAccountRef accountRef) {
        var locked = repo.findByIdAccountTypeAndIdOwnerRefForUpdate(accountRef.type().name(), accountRef.ownerRef());
        if (locked.isEmpty()) {
            throw new IllegalStateException("Account profile not found for lock: " + accountRef);
        }
    }
}
