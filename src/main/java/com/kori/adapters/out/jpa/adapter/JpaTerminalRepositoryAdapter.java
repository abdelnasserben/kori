package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.TerminalEntity;
import com.kori.adapters.out.jpa.repo.TerminalJpaRepository;
import com.kori.application.port.out.TerminalRepositoryPort;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.MerchantId;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import com.kori.domain.model.terminal.TerminalUid;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public Optional<Terminal> findById(TerminalId terminalId) {
        Objects.requireNonNull(terminalId, "terminalUid must not be null");
        return repo.findById(terminalId.value())
                .map(this::toDomain);
    }

    @Override
    public void save(Terminal terminal) {
        repo.save(new TerminalEntity(
                terminal.id().value(),
                terminal.terminalUid().value(),
                terminal.merchantId().value(),
                terminal.status().name(),
                terminal.createdAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Terminal> findByMerchantId(MerchantId merchantId) {
        return repo.findAllByMerchantId(merchantId.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Terminal> findByUid(TerminalUid terminalUid) {
        return repo.findByTerminalUid(terminalUid.value())
                .map(this::toDomain);
    }

    @Override
    public boolean existsByUid(TerminalUid terminalUid) {
        return repo.existsByTerminalUid(terminalUid.value());
    }

    private Terminal toDomain(TerminalEntity entity) {
        return new Terminal(
                new TerminalId(entity.getId()),
                TerminalUid.of(entity.getTerminalUid()),
                new MerchantId(entity.getMerchantId()),
                Status.valueOf(entity.getStatus()),
                entity.getCreatedAt()
        );
    }

}
