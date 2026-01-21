package com.kori.application.port.out;

import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;

import java.util.Optional;

public interface ClientRepositoryPort {
    Optional<Client> findByPhoneNumber(String phoneNumber);

    Optional<Client> findById(ClientId clientId);

    Client save(Client client);
}
