package com.kori.domain.ledger;

import java.util.Objects;

/**
 * Référence de compte ledger.
 * - Pour les comptes "par titulaire" : ownerRef = cardUid/merchantCode/agentCode (String)
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

    public static LedgerAccountRef client(String clientId) {
        return new LedgerAccountRef(LedgerAccountType.CLIENT, clientId);
    }

    public static LedgerAccountRef merchant(String merchantId) {
        return new LedgerAccountRef(LedgerAccountType.MERCHANT, merchantId);
    }

    public static LedgerAccountRef agent(String agentId) {
        return new LedgerAccountRef(LedgerAccountType.AGENT, agentId);
    }

    public static LedgerAccountRef agentWallet(String agentId) {
        return new LedgerAccountRef(LedgerAccountType.AGENT_WALLET, agentId);
    }

    public static LedgerAccountRef agentCashClearing(String agentId) {
        return new LedgerAccountRef(LedgerAccountType.AGENT_CASH_CLEARING, agentId);
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
        return this.type == LedgerAccountType.AGENT
                || this.type == LedgerAccountType.AGENT_WALLET
                || this.type == LedgerAccountType.AGENT_CASH_CLEARING;
    }
}
