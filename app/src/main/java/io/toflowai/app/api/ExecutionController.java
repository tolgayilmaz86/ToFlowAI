package io.toflowai.app.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.common.dto.ExecutionDTO;

/**
 * REST API controller for workflow executions.
 */
@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(final ExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping
    public List<ExecutionDTO> findAll() {
        return executionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExecutionDTO> findById(@PathVariable final Long id) {
        return executionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/workflow/{workflowId}")
    public List<ExecutionDTO> findByWorkflowId(@PathVariable final Long workflowId) {
        return executionService.findByWorkflowId(workflowId);
    }

    @GetMapping("/running")
    public List<ExecutionDTO> findRunning() {
        return executionService.findRunningExecutions();
    }

    @PostMapping("/run/{workflowId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<ExecutionDTO> executeWorkflow(
            @PathVariable final Long workflowId,
            @RequestBody(required = false) final Map<String, Object> input) {
        return executionService.executeAsync(workflowId, input != null ? input : Map.of());
    }

    @PostMapping("/run/{workflowId}/sync")
    public ExecutionDTO executeWorkflowSync(
            @PathVariable final Long workflowId,
            @RequestBody(required = false) final Map<String, Object> input) {
        return executionService.execute(workflowId, input != null ? input : Map.of());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable final Long id) {
        executionService.cancel(id);
        return ResponseEntity.ok().build();
    }
}
