package com.kori.application.handler;

import com.kori.application.events.MerchantStatusChangedEvent;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.port.out.TerminalRepositoryPort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.terminal.Terminal;

import java.util.List;
import java.util.Objects;

/**
 * Propagation du changement de statut Merchant :
 * - SUSPENDED => suspend AccountProfile(merchant) + suspend terminals ACTIVE
 * - CLOSED    => close AccountProfile(merchant) + close terminals
 * Aucun "auto-reactivate" des terminals lors d'un retour à ACTIVE.
 */
public class OnMerchantStatusChangedHandler {

    private final AccountProfilePort accountProfilePort;
    private final TerminalRepositoryPort terminalRepositoryPort;

    public OnMerchantStatusChangedHandler(AccountProfilePort accountProfilePort,
                                          TerminalRepositoryPort terminalRepositoryPort) {
        this.accountProfilePort = Objects.requireNonNull(accountProfilePort);
        this.terminalRepositoryPort = Objects.requireNonNull(terminalRepositoryPort);
    }

    public void handle(MerchantStatusChangedEvent event) {
        Objects.requireNonNull(event, "event");

        if (event.before() == event.after()) {
            return;
        }

        Status after = event.after();

        // 1) AccountProfile merchant
        LedgerAccountRef merchantAccountRef =
                LedgerAccountRef.merchant(event.merchantId().value().toString());

        AccountProfile merchantProfile =
                accountProfilePort.findByAccount(merchantAccountRef).orElse(null);

        if (merchantProfile != null) {
            switch (after) {
                case SUSPENDED -> {
                    merchantProfile.suspend();
                    accountProfilePort.save(merchantProfile);
                }
                case CLOSED -> {
                    merchantProfile.close();
                    accountProfilePort.save(merchantProfile);
                }
                case ACTIVE -> {
                    // OK de réactiver le compte, pas les terminaux
                    merchantProfile.activate();
                    accountProfilePort.save(merchantProfile);
                }
            }
        }

        // 2) Terminals
        List<Terminal> terminals =
                terminalRepositoryPort.findByMerchantId(event.merchantId());

        switch (after) {
            case SUSPENDED -> {
                for (Terminal terminal : terminals) {
                    if (terminal.status() == Status.ACTIVE) {
                        terminal.suspend();
                        terminalRepositoryPort.save(terminal);
                    }
                }
            }
            case CLOSED -> {
                for (Terminal terminal : terminals) {
                    if (terminal.status() != Status.CLOSED) {
                        terminal.close();
                        terminalRepositoryPort.save(terminal);
                    }
                }
            }
        }
        // IMPORTANT : pas de réactivation automatique des terminaux
    }
}
