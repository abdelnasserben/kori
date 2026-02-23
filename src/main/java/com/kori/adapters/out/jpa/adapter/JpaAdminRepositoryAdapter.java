package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.AdminEntity;
import com.kori.adapters.out.jpa.repo.AdminJpaRepository;
import com.kori.application.port.out.AdminRepositoryPort;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.admin.AdminId;
import com.kori.domain.model.admin.AdminUsername;
import com.kori.domain.model.common.DisplayName;
import com.kori.domain.model.common.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaAdminRepositoryAdapter implements AdminRepositoryPort {

    private final AdminJpaRepository repo;

    public JpaAdminRepositoryAdapter(AdminJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Admin> findById(AdminId adminId) {
        return repo.findById(adminId.value())
                .map(entity -> new Admin(
                        new AdminId(entity.getId()),
                        AdminUsername.of(entity.getUsername()),
                        DisplayName.ofNullable(entity.getDisplayName()),
                        Status.valueOf(entity.getStatus()),
                        entity.getCreatedAt()
                ));
    }

    @Override
    public void save(Admin admin) {
        repo.save(new AdminEntity(
                admin.id().value(),
                admin.username().value(),
                admin.displayName() == null ? null : admin.displayName().value(),
                admin.status().name(),
                admin.createdAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Admin> findByUsername(String username) {
        return repo.findByUsername(username)
                .map(entity -> new Admin(
                        new AdminId(entity.getId()),
                        AdminUsername.of(entity.getUsername()),
                        DisplayName.ofNullable(entity.getDisplayName()),
                        Status.valueOf(entity.getStatus()),
                        entity.getCreatedAt()
                ));
    }
}
