package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.ClientEntity;
import com.kori.adapters.out.jpa.repo.ClientJpaRepository;
import com.kori.application.port.out.ClientRepositoryPort;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientCode;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.client.PhoneNumber;
import com.kori.domain.model.common.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

@Component
public class JpaClientRepositoryAdapter implements ClientRepositoryPort {

    private final ClientJpaRepository repo;

    public JpaClientRepositoryAdapter(ClientJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> findByPhoneNumber(PhoneNumber phoneNumber) {
        return repo.findByPhoneNumber(phoneNumber.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> findByCode(ClientCode code) {
        return repo.findByCode(code.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(ClientCode code) {
        return repo.existsByCode(code.value());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> findById(ClientId clientId) {
        return repo.findById(clientId.value()).map(this::toDomain);
    }

    @Override
    @Transactional
    public Client save(Client client) {
        ClientEntity e = new ClientEntity(
                client.id().value(),
                client.phoneNumber().value(),
                client.code().value(),
                client.status().name(),
                client.createdAt().atOffset(ZoneOffset.UTC)
        );
        repo.save(e);
        return client;
    }

    private Client toDomain(ClientEntity e) {
        return new Client(
                new ClientId(e.getId()),
                ClientCode.of(e.getCode()),
                PhoneNumber.of(e.getPhoneNumber()),
                Status.valueOf(e.getStatus()),
                e.getCreatedAt().toInstant()
        );
    }
}
