package com.kori.application.port.out;

import com.kori.domain.model.config.PlatformConfig;

import java.util.Optional;

public interface PlatformConfigPort {
    Optional<PlatformConfig> get();

    void upsert(PlatformConfig config);
}
