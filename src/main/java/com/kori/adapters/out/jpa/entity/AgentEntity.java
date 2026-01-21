package com.kori.adapters.out.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "agents")
@Access(AccessType.FIELD)
public class AgentEntity {
    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    protected AgentEntity() { }
    public AgentEntity(String id) { this.id = id; }
    public String getId() { return id; }
}
