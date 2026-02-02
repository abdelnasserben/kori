package com.kori.application.port.out;

import com.kori.domain.model.config.CommissionConfig;

import java.util.Optional;

public interface CommissionConfigPort {
    Optional<CommissionConfig> get();

    void upsert(CommissionConfig config);
}
