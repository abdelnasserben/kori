package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.AccountProfileEntity;
import com.kori.adapters.out.jpa.repo.AccountProfileJpaRepository;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaAccountProfileAdapter implements AccountProfilePort {

    private final AccountProfileJpaRepository repo;

    public JpaAccountProfileAdapter(AccountProfileJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountProfile> findByAccount(LedgerAccountRef account) {
        return repo.findByIdAccountTypeAndIdOwnerRef(account.type().name(), account.ownerRef())
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(AccountProfile profile) {
        AccountProfileEntity e = new AccountProfileEntity(
                new AccountProfileEntity.AccountProfileId(
                        profile.account().type().name(),
                        profile.account().ownerRef()
                ),
                profile.status().name(),
                profile.createdAt()
        );
        repo.save(e);
    }

    private AccountProfile toDomain(AccountProfileEntity e) {
        LedgerAccountRef account = new LedgerAccountRef(
                LedgerAccountType.valueOf(e.getId().getAccountType()),
                e.getId().getOwnerRef()
        );

        return new AccountProfile(
                account,
                e.getCreatedAt(),
                Status.valueOf(e.getStatus())
        );
    }
}
