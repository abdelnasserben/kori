package com.kori.application.port.out;

import com.kori.domain.model.client.Client;

import java.util.Optional;

public interface ClientRepositoryPort {
    Optional<Client> findByPhoneNumber(String phoneNumber);

    Client save(Client client);
}
