package com.kori.application.port.out;

import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;

import java.util.Optional;

public interface PayoutRepositoryPort {
    Payout save(Payout payout);

    Optional<Payout> findById(PayoutId payoutId);

    boolean existsRequestedForAgent(String agentId);
}
