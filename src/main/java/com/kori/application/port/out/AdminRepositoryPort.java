package com.kori.application.port.out;

import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.admin.AdminId;

import java.util.Optional;

public interface AdminRepositoryPort {
    Optional<Admin> findById(AdminId adminId);

    void save(Admin admin);

    Optional<Admin> findByUsername(String username);
}
