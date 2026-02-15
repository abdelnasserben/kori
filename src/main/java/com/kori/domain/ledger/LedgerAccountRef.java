package com.kori.domain.ledger;

import java.util.Objects;

/**
 * Référence de compte ledger.
 * - Pour les comptes "par titulaire" : ownerRef = publicRef métier (phoneNumber/merchantCode/agentCode/terminalUid)
 * - Pour les comptes système : ownerRef = "SYSTEM"
 */
public record LedgerAccountRef(LedgerAccountType type, String ownerRef) {
    public LedgerAccountRef(LedgerAccountType type, String ownerRef) {
        this.type = Objects.requireNonNull(type, "type");
        this.ownerRef = Objects.requireNonNull(ownerRef, "ownerRef").trim();
        if (this.ownerRef.isBlank()) {
            throw new IllegalArgumentException("ownerRef must not be blank");
        }
    }

    public static LedgerAccountRef client(String clientRef) {
        return new LedgerAccountRef(LedgerAccountType.CLIENT, clientRef);
    }

    public static LedgerAccountRef merchant(String merchantRef) {
        return new LedgerAccountRef(LedgerAccountType.MERCHANT, merchantRef);
    }

    public static LedgerAccountRef agentWallet(String agentRef) {
        return new LedgerAccountRef(LedgerAccountType.AGENT_WALLET, agentRef);
    }

    public static LedgerAccountRef agentCashClearing(String agentRef) {
        return new LedgerAccountRef(LedgerAccountType.AGENT_CASH_CLEARING, agentRef);
    }

    public static LedgerAccountRef platformFeeRevenue() {
        return new LedgerAccountRef(LedgerAccountType.PLATFORM_FEE_REVENUE, "SYSTEM");
    }

    public static LedgerAccountRef platformClearing() {
        return new LedgerAccountRef(LedgerAccountType.PLATFORM_CLEARING, "SYSTEM");
    }

    public static LedgerAccountRef platformBank() {
        return new LedgerAccountRef(LedgerAccountType.PLATFORM_BANK, "SYSTEM");
    }

    public static LedgerAccountRef platformClientRefundClearing() {
        return new LedgerAccountRef(LedgerAccountType.PLATFORM_CLIENT_REFUND_CLEARING, "SYSTEM");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LedgerAccountRef other)) return false;
        return type == other.type && ownerRef.equals(other.ownerRef);
    }

    @Override
    public String toString() {
        return type + ":" + ownerRef;
    }

    public boolean isForClient() {
        return this.type == LedgerAccountType.CLIENT;
    }

    public boolean isForMerchant() {
        return this.type == LedgerAccountType.MERCHANT;
    }

    public boolean isForAgent() {
        return this.type == LedgerAccountType.AGENT_WALLET
                || this.type == LedgerAccountType.AGENT_CASH_CLEARING;
    }
}
