package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.repo.SecurityConfigJpaRepository;
import com.kori.application.port.out.CardSecurityPolicyPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class JpaCardSecurityPolicyAdapter implements CardSecurityPolicyPort {

    private final SecurityConfigJpaRepository repo;

    public JpaCardSecurityPolicyAdapter(SecurityConfigJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public int maxFailedPinAttempts() {
        return repo.findById(1)
                .orElseThrow(() -> new IllegalStateException("security_config row id=1 missing"))
                .getMaxFailedPinAttempts();
    }
}
