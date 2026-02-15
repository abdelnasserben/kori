package com.kori.application.port.out;

import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientCode;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.client.PhoneNumber;

import java.util.Optional;

public interface ClientRepositoryPort {
    Optional<Client> findByPhoneNumber(PhoneNumber phoneNumber);

    Optional<Client> findByCode(ClientCode code);

    boolean existsByCode(ClientCode code);

    Optional<Client> findById(ClientId clientId);

    Client save(Client client);
}
