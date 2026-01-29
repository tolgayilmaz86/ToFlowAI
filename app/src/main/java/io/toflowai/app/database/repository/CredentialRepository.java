package io.toflowai.app.database.repository;

import io.toflowai.app.database.model.CredentialEntity;
import io.toflowai.common.enums.CredentialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Credential entities.
 */
@Repository
public interface CredentialRepository extends JpaRepository<CredentialEntity, Long> {

    /**
     * Find credential by name.
     */
    Optional<CredentialEntity> findByName(String name);

    /**
     * Find credentials by type.
     */
    List<CredentialEntity> findByType(CredentialType type);

    /**
     * Find credentials by name containing (search).
     */
    List<CredentialEntity> findByNameContainingIgnoreCase(String name);

    /**
     * Check if credential with name exists.
     */
    boolean existsByName(String name);
}
