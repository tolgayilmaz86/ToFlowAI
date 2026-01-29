package io.toflowai.app.api;

import io.toflowai.app.service.CredentialService;
import io.toflowai.common.dto.CredentialDTO;
import io.toflowai.common.enums.CredentialType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for credential management.
 */
@RestController
@RequestMapping("/api/credentials")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    public List<CredentialDTO> findAll() {
        return credentialService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CredentialDTO> findById(@PathVariable Long id) {
        return credentialService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public List<CredentialDTO> findByType(@PathVariable CredentialType type) {
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
    public CredentialDTO create(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        CredentialType type = CredentialType.valueOf((String) request.get("type"));
        String data = (String) request.get("data");
        
        CredentialDTO dto = CredentialDTO.create(name, type);
        return credentialService.create(dto, data);
    }

    /**
     * Update a credential.
     * Data field is optional - if not provided, only metadata is updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CredentialDTO> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        CredentialType type = CredentialType.valueOf((String) request.get("type"));
        String data = (String) request.get("data");
        
        CredentialDTO dto = CredentialDTO.create(name, type);
        return ResponseEntity.ok(credentialService.update(id, dto, data));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        credentialService.delete(id);
    }
}
