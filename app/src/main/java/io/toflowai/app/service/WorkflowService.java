package io.toflowai.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.toflowai.app.database.model.WorkflowEntity;
import io.toflowai.app.database.repository.WorkflowRepository;
import io.toflowai.common.domain.Connection;
import io.toflowai.common.domain.Node;
import io.toflowai.common.dto.WorkflowDTO;
import io.toflowai.common.enums.TriggerType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing workflows.
 */
@Service
@Transactional
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;

    public WorkflowService(WorkflowRepository workflowRepository, ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.objectMapper = objectMapper;
    }

    public List<WorkflowDTO> findAll() {
        return workflowRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<WorkflowDTO> findById(Long id) {
        return workflowRepository.findById(id)
                .map(this::toDTO);
    }

    public List<WorkflowDTO> findByTriggerType(TriggerType triggerType) {
        return workflowRepository.findByTriggerType(triggerType).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<WorkflowDTO> findActiveScheduledWorkflows() {
        return workflowRepository.findActiveScheduledWorkflows().stream()
                .map(this::toDTO)
                .toList();
    }

    public WorkflowDTO create(WorkflowDTO dto) {
        WorkflowEntity entity = toEntity(dto);
        // ID is auto-generated, createdAt/updatedAt handled by @PrePersist
        WorkflowEntity saved = workflowRepository.save(entity);
        return toDTO(saved);
    }

    public WorkflowDTO update(WorkflowDTO dto) {
        if (dto.id() == null) {
            throw new IllegalArgumentException("Workflow ID cannot be null for update");
        }
        WorkflowEntity existing = workflowRepository.findById(dto.id())
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + dto.id()));

        existing.setName(dto.name());
        existing.setDescription(dto.description());
        existing.setNodesJson(serializeNodes(dto.nodes()));
        existing.setConnectionsJson(serializeConnections(dto.connections()));
        existing.setSettingsJson(serializeSettings(dto.settings()));
        existing.setActive(dto.isActive());
        existing.setTriggerType(dto.triggerType());
        existing.setCronExpression(dto.cronExpression());
        // updatedAt handled by @PreUpdate

        WorkflowEntity saved = workflowRepository.save(existing);
        return toDTO(saved);
    }

    public void delete(Long id) {
        workflowRepository.deleteById(id);
    }

    public WorkflowDTO duplicate(Long id) {
        WorkflowDTO original = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));

        WorkflowDTO copy = new WorkflowDTO(
                null,
                original.name() + " (Copy)",
                original.description(),
                original.nodes(),
                original.connections(),
                original.settings(),
                false,
                original.triggerType(),
                original.cronExpression(),
                null, null, null, 0
        );

        return create(copy);
    }

    public void setActive(Long id, boolean active) {
        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        entity.setActive(active);
        workflowRepository.save(entity);
    }

    private WorkflowDTO toDTO(WorkflowEntity entity) {
        List<Node> nodes = parseNodes(entity.getNodesJson());
        List<Connection> connections = parseConnections(entity.getConnectionsJson());
        Map<String, Object> settings = parseSettings(entity.getSettingsJson());

        return new WorkflowDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                nodes,
                connections,
                settings,
                entity.isActive(),
                entity.getTriggerType(),
                entity.getCronExpression(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastExecuted(),
                entity.getVersion()
        );
    }

    private WorkflowEntity toEntity(WorkflowDTO dto) {
        WorkflowEntity entity = new WorkflowEntity(dto.name());
        entity.setDescription(dto.description());
        entity.setNodesJson(serializeNodes(dto.nodes()));
        entity.setConnectionsJson(serializeConnections(dto.connections()));
        entity.setSettingsJson(serializeSettings(dto.settings()));
        entity.setActive(dto.isActive());
        entity.setTriggerType(dto.triggerType());
        entity.setCronExpression(dto.cronExpression());
        return entity;
    }

    private List<Node> parseNodes(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse nodes JSON", e);
        }
    }

    private List<Connection> parseConnections(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse connections JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSettings(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse settings JSON", e);
        }
    }

    private String serializeNodes(List<Node> nodes) {
        if (nodes == null) return "[]";
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize nodes", e);
        }
    }

    private String serializeConnections(List<Connection> connections) {
        if (connections == null) return "[]";
        try {
            return objectMapper.writeValueAsString(connections);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize connections", e);
        }
    }

    private String serializeSettings(Map<String, Object> settings) {
        if (settings == null) return null;
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize settings", e);
        }
    }
}
