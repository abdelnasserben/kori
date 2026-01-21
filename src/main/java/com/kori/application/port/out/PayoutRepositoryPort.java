package com.kori.application.port.out;

import com.kori.domain.model.payout.Payout;

public interface PayoutRepositoryPort {
    Payout save(Payout payout);
}
