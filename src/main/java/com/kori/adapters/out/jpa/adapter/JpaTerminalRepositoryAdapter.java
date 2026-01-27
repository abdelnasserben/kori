package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.TerminalEntity;
import com.kori.adapters.out.jpa.repo.TerminalJpaRepository;
import com.kori.application.port.out.TerminalRepositoryPort;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.MerchantId;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaTerminalRepositoryAdapter implements TerminalRepositoryPort {

    private final TerminalJpaRepository repo;

    public JpaTerminalRepositoryAdapter(TerminalJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Terminal> findById(String terminalId) {
        Objects.requireNonNull(terminalId, "terminalId must not be null");
        return repo.findById(terminalId)
                .map(e -> new Terminal(
                        new TerminalId(e.getId()),
                        new MerchantId(e.getMerchantId()),
                        Status.valueOf(e.getStatus())
                )
        );
    }

    @Override
    public void save(Terminal terminal) {
        repo.save(new TerminalEntity(
                terminal.id().value(),
                terminal.merchantId().value(),
                terminal.status().name()
        ));
    }
}
