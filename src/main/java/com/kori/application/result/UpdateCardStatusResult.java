package com.kori.application.result;

import java.util.UUID;

public record UpdateCardStatusResult(
        UUID cardUid,
        String previousStatus,
        String newStatus
) {}
