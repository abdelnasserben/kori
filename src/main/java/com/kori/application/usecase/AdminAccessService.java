package com.kori.application.usecase;

import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorStatusGuards;
import com.kori.application.guard.ActorTypeGuards;
import com.kori.application.port.out.AdminRepositoryPort;
import com.kori.application.security.ActorContext;
import com.kori.domain.model.admin.Admin;

public class AdminAccessService {

    private final AdminRepositoryPort adminRepositoryPort;

    public AdminAccessService(AdminRepositoryPort adminRepositoryPort) {
        this.adminRepositoryPort = adminRepositoryPort;
    }

    /**
     * Retourne l'admin courant (chargé depuis la persistence) après validations.
     *
     * @param actorContext contexte de l'acteur
     * @param action       libellé pour tracer l'action (ex : "create admin")
     */
    public void requireActiveAdmin(ActorContext actorContext, String action) {
        ActorTypeGuards.onlyAdminCan(actorContext, action);

        Admin admin = adminRepositoryPort.findByUsername(actorContext.actorRef())
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        ActorStatusGuards.requireActiveAdmin(admin);
    }
}
