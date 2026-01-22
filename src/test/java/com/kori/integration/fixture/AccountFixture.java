package com.kori.integration.fixture;

import com.kori.adapters.out.jpa.entity.AccountEntity;
import com.kori.adapters.out.jpa.repo.AccountJpaRepository;

import java.util.UUID;

/**
 * Fixture JPA dédiée à "accounts".
 *
 * Objectifs :
 * - Créer des comptes facilement via le repository JPA (pas de SQL ici)
 * - Centraliser la création d'un Account lié à un client existant (FK clients -> accounts)
 * - Garder les tests courts et lisibles
 *
 * Note: les tests étant @Transactional, tout est rollback automatiquement.
 */
public final class AccountFixture {

    private final AccountJpaRepository accountJpaRepository;

    public AccountFixture(AccountJpaRepository accountJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
    }

    /**
     * Crée et persiste un compte "ACTIVE" pour un client existant.
     */
    public AccountEntity createActiveForClient(UUID clientId) {
        return createForClient(clientId, "ACTIVE");
    }

    /**
     * Crée et persiste un compte pour un client existant.
     * Attention: la colonne client_id est UNIQUE => un seul compte par client.
     */
    public AccountEntity createForClient(UUID clientId, String status) {
        UUID accountId = UUID.randomUUID();
        AccountEntity entity = new AccountEntity(accountId, clientId, status);
        return accountJpaRepository.saveAndFlush(entity);
    }

    /**
     * Permet de persister un account avec un id imposé (utile si certains tests fixent l'UUID).
     */
    public AccountEntity create(UUID accountId, UUID clientId, String status) {
        AccountEntity entity = new AccountEntity(accountId, clientId, status);
        return accountJpaRepository.saveAndFlush(entity);
    }
}
