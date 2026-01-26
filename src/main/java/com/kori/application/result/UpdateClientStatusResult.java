package com.kori.application.result;

import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;

import java.util.UUID;

public record UpdateClientStatusResult(
        ClientId clientId,
        Status previousStatus,
        Status newStatus
) {}
