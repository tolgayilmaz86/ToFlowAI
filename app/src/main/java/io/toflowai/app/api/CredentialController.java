package io.toflowai.app.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.toflowai.app.service.CredentialService;
import io.toflowai.common.dto.CredentialDTO;
import io.toflowai.common.enums.CredentialType;

/**
 * REST API controller for credential management.
 */
@RestController
@RequestMapping("/api/credentials")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(final CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    public List<CredentialDTO> findAll() {
        return credentialService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CredentialDTO> findById(@PathVariable final Long id) {
        return credentialService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public List<CredentialDTO> findByType(@PathVariable final CredentialType type) {
        return credentialService.findByType(type);
    }

    /**
     * Create a new credential.
     * Request body should contain:
     * - name: Credential name
     * - type: Credential type
     * - data: The actual credential data (will be encrypted)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialDTO create(@RequestBody final Map<String, Object> request) {
        final String name = (String) request.get("name");
        final CredentialType type = CredentialType.valueOf((String) request.get("type"));
        final String data = (String) request.get("data");

        final CredentialDTO dto = CredentialDTO.create(name, type);
        return credentialService.create(dto, data);
    }

    /**
     * Update a credential.
     * Data field is optional - if not provided, only metadata is updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CredentialDTO> update(
            @PathVariable final Long id,
            @RequestBody final Map<String, Object> request) {
        final String name = (String) request.get("name");
        final CredentialType type = CredentialType.valueOf((String) request.get("type"));
        final String data = (String) request.get("data");

        final CredentialDTO dto = CredentialDTO.create(name, type);
        return ResponseEntity.ok(credentialService.update(id, dto, data));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final Long id) {
        credentialService.delete(id);
    }
}
