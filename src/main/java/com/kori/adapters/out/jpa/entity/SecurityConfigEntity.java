package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "security_config")
@Access(AccessType.FIELD)
public class SecurityConfigEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "max_failed_pin_attempts", nullable = false)
    private Integer maxFailedPinAttempts;

    protected SecurityConfigEntity() { }

}
