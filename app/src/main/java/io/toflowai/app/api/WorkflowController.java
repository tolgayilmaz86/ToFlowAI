package io.toflowai.app.api;

import io.toflowai.app.service.WorkflowService;
import io.toflowai.common.dto.WorkflowDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for workflow management.
 */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    public List<WorkflowDTO> findAll() {
        return workflowService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDTO> findById(@PathVariable Long id) {
        return workflowService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDTO create(@RequestBody WorkflowDTO dto) {
        return workflowService.create(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDTO> update(@PathVariable Long id, @RequestBody WorkflowDTO dto) {
        if (!id.equals(dto.id())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(workflowService.update(dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workflowService.delete(id);
    }

    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDTO duplicate(@PathVariable Long id) {
        return workflowService.duplicate(id);
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> setActive(@PathVariable Long id, @RequestParam boolean active) {
        workflowService.setActive(id, active);
        return ResponseEntity.ok().build();
    }
}
