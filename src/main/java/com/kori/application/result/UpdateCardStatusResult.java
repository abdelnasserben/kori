package com.kori.application.result;

import com.kori.domain.model.card.CardStatus;

import java.util.UUID;

public record UpdateCardStatusResult(
        UUID cardUid,
        CardStatus previousStatus,
        CardStatus newStatus
) {}
