package io.toflowai.app.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.toflowai.app.service.WorkflowService;
import io.toflowai.common.dto.WorkflowDTO;

/**
 * REST API controller for workflow management.
 */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(final WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    public List<WorkflowDTO> findAll() {
        return workflowService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDTO> findById(@PathVariable final Long id) {
        return workflowService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDTO create(@RequestBody final WorkflowDTO dto) {
        return workflowService.create(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDTO> update(@PathVariable final Long id, @RequestBody final WorkflowDTO dto) {
        if (!id.equals(dto.id())) {
            return ResponseEntity.badRequest().build();
        }
        final WorkflowDTO updated = workflowService.update(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final Long id) {
        workflowService.delete(id);
    }

    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDTO duplicate(@PathVariable final Long id) {
        return workflowService.duplicate(id);
    }

    @PatchMapping("/{id}/active")
    @ResponseStatus(HttpStatus.OK)
    public void activate(@PathVariable final Long id, @RequestParam final boolean active) {
        workflowService.setActive(id, active);
    }
}
