package com.kori.application.port.out;

import com.kori.domain.model.config.FeeConfig;

import java.util.Optional;

public interface FeeConfigPort {
    Optional<FeeConfig> get();

    void upsert(FeeConfig config);
}
