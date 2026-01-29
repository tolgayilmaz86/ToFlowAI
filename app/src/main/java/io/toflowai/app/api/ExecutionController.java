package io.toflowai.app.api;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.common.dto.ExecutionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST API controller for workflow executions.
 */
@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping
    public List<ExecutionDTO> findAll() {
        return executionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExecutionDTO> findById(@PathVariable Long id) {
        return executionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/workflow/{workflowId}")
    public List<ExecutionDTO> findByWorkflowId(@PathVariable Long workflowId) {
        return executionService.findByWorkflowId(workflowId);
    }

    @GetMapping("/running")
    public List<ExecutionDTO> findRunning() {
        return executionService.findRunningExecutions();
    }

    @PostMapping("/run/{workflowId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<ExecutionDTO> executeWorkflow(
            @PathVariable Long workflowId,
            @RequestBody(required = false) Map<String, Object> input) {
        return executionService.executeAsync(workflowId, input != null ? input : Map.of());
    }

    @PostMapping("/run/{workflowId}/sync")
    public ExecutionDTO executeWorkflowSync(
            @PathVariable Long workflowId,
            @RequestBody(required = false) Map<String, Object> input) {
        return executionService.execute(workflowId, input != null ? input : Map.of());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        executionService.cancel(id);
        return ResponseEntity.ok().build();
    }
}
