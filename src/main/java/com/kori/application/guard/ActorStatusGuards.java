package com.kori.application.guard;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.terminal.Terminal;

public final class ActorStatusGuards {
    public static void requireActiveAdmin(Admin admin) {
        requireActive(admin.status(), "ADMIN_NOT_ACTIVE");
    }

    public static void requireActiveAgent(Agent agent) {
        requireActive(agent.status(), "AGENT_NOT_ACTIVE");
    }

    public static void requireActiveClient(Client client) {
        requireActive(client.status(), "CLIENT_NOT_ACTIVE");
    }

    public static void requireActiveMerchant(Merchant merchant) {
        requireActive(merchant.status(), "MERCHANT_NOT_ACTIVE");
    }

    public static void requireActiveTerminal(Terminal terminal) {
        requireActive(terminal.status(), "TERMINAL_NOT_ACTIVE");
    }

    private static void requireActive(Status status, String code) {
        if (status != Status.ACTIVE) {
            throw new ForbiddenOperationException(code);
        }
    }
}
