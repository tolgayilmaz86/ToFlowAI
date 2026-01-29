package io.toflowai.app.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import io.toflowai.common.dto.VariableDTO.VariableScope;
import io.toflowai.common.dto.VariableDTO.VariableType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entity for storing workflow variables.
 */
@Entity
@Table(name = "variables", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "var_scope", "workflow_id" })
})
public class VariableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "var_value", columnDefinition = "CLOB")
    private String value;

    @Column(name = "encrypted_value", columnDefinition = "CLOB")
    private String encryptedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "var_type", nullable = false)
    private VariableType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "var_scope", nullable = false)
    private VariableScope scope;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public VariableEntity() {
    }

    public VariableEntity(String name, String value, VariableType type, VariableScope scope) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.scope = scope;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    public VariableType getType() {
        return type;
    }

    public void setType(VariableType type) {
        this.type = type;
    }

    public VariableScope getScope() {
        return scope;
    }

    public void setScope(VariableScope scope) {
        this.scope = scope;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Check if this variable stores encrypted data.
     */
    public boolean isEncrypted() {
        return type == VariableType.SECRET && encryptedValue != null;
    }
}
