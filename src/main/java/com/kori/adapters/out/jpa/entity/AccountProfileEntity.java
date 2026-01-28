package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "account_profiles",
        indexes = {
                @Index(name = "idx_account_profiles_status", columnList = "status")
        }
)
@Access(AccessType.FIELD)
public class AccountProfileEntity {

    @EmbeddedId
    private AccountProfileId id;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccountProfileEntity() {
        // for JPA
    }

    public AccountProfileEntity(AccountProfileId id, String status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    @Getter
    @Embeddable
    public static class AccountProfileId implements Serializable {

        @Column(name = "account_type", nullable = false, updatable = false, length = 32)
        private String accountType;

        @Column(name = "owner_ref", nullable = false, updatable = false, length = 64)
        private String ownerRef;

        protected AccountProfileId() {
            // for JPA
        }

        public AccountProfileId(String accountType, String ownerRef) {
            String t = Objects.requireNonNull(accountType, "accountType").trim();
            if (t.isBlank()) throw new IllegalArgumentException("accountType must not be blank");
            this.accountType = t;

            String o = Objects.requireNonNull(ownerRef, "ownerRef").trim();
            if (o.isBlank()) throw new IllegalArgumentException("ownerRef must not be blank");
            this.ownerRef = o;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AccountProfileId other)) return false;
            return accountType.equals(other.accountType) && ownerRef.equals(other.ownerRef);
        }

        @Override
        public int hashCode() {
            int result = accountType.hashCode();
            result = 31 * result + ownerRef.hashCode();
            return result;
        }
    }
}
