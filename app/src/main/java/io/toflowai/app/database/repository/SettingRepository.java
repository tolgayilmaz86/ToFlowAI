package io.toflowai.app.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.toflowai.app.database.model.SettingEntity;
import io.toflowai.common.enums.SettingCategory;

/**
 * Repository for Setting entities.
 */
@Repository
public interface SettingRepository extends JpaRepository<SettingEntity, Long> {

    /**
     * Find setting by unique key.
     */
    Optional<SettingEntity> findByKey(String key);

    /**
     * Find all settings in a category.
     */
    List<SettingEntity> findByCategoryOrderByDisplayOrderAsc(SettingCategory category);

    /**
     * Find all visible settings in a category.
     */
    List<SettingEntity> findByCategoryAndVisibleTrueOrderByDisplayOrderAsc(SettingCategory category);

    /**
     * Find all visible settings.
     */
    List<SettingEntity> findByVisibleTrueOrderByCategoryAscDisplayOrderAsc();

    /**
     * Find settings by key prefix (e.g., "ai.openai." for all OpenAI settings).
     */
    @Query("SELECT s FROM SettingEntity s WHERE s.key LIKE :prefix% ORDER BY s.displayOrder ASC")
    List<SettingEntity> findByKeyPrefix(String prefix);

    /**
     * Delete setting by key.
     */
    void deleteByKey(String key);

    /**
     * Check if setting exists by key.
     */
    boolean existsByKey(String key);

    /**
     * Find settings requiring restart.
     */
    List<SettingEntity> findByRequiresRestartTrue();

    /**
     * Search settings by key or label.
     */
    @Query("SELECT s FROM SettingEntity s WHERE s.visible = true AND " +
            "(LOWER(s.key) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.label) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<SettingEntity> searchSettings(String query);
}
