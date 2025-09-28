package com.enterprise.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface providing common operations for all entities.
 * Extends JpaRepository and JpaSpecificationExecutor for comprehensive data access.
 * 
 * Requirements addressed:
 * - 5.1: CRUD operations, custom queries, and specifications
 * - 5.2: @Query annotations and method name derivation
 * - 5.3: Pageable and Sort parameters
 * - 5.4: Batch operations
 * - 3.6: Soft delete and active record filtering
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Find all active (non-deleted) entities.
     * 
     * @return list of active entities
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
    List<T> findAllActive();

    /**
     * Find all active entities with pagination.
     * 
     * @param pageable pagination information
     * @return page of active entities
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
    Page<T> findAllActive(Pageable pageable);

    /**
     * Find an active entity by ID.
     * 
     * @param id the entity ID
     * @return optional containing the entity if found and active
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
    Optional<T> findByIdActive(@Param("id") ID id);

    /**
     * Check if an active entity exists by ID.
     * 
     * @param id the entity ID
     * @return true if active entity exists
     */
    @Query("SELECT COUNT(e) > 0 FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
    boolean existsByIdActive(@Param("id") ID id);

    /**
     * Soft delete an entity by setting deleted flag to true.
     * 
     * @param id the entity ID to soft delete
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = true, e.lastModifiedDate = CURRENT_TIMESTAMP WHERE e.id = :id")
    int softDelete(@Param("id") ID id);

    /**
     * Soft delete multiple entities by IDs.
     * 
     * @param ids the entity IDs to soft delete
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = true, e.lastModifiedDate = CURRENT_TIMESTAMP WHERE e.id IN :ids")
    int softDeleteByIds(@Param("ids") List<ID> ids);

    /**
     * Restore a soft-deleted entity.
     * 
     * @param id the entity ID to restore
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = false, e.lastModifiedDate = CURRENT_TIMESTAMP WHERE e.id = :id")
    int restore(@Param("id") ID id);

    /**
     * Find all entities including deleted ones with pagination.
     * This method will be implemented by concrete repositories to bypass @Where filtering.
     * 
     * @param pageable pagination information
     * @return page of all entities
     */
    @Query("SELECT e FROM #{#entityName} e")
    Page<T> findAllWithDeleted(Pageable pageable);

    /**
     * Find all deleted entities using method name derivation.
     * This bypasses the @Where clause filtering.
     * 
     * @return list of deleted entities
     */
    List<T> findByDeletedTrue();

    /**
     * Find all deleted entities with pagination using method name derivation.
     * This bypasses the @Where clause filtering.
     * 
     * @param pageable pagination information
     * @return page of deleted entities
     */
    Page<T> findByDeletedTrue(Pageable pageable);

    /**
     * Count active entities using method name derivation.
     * 
     * @return count of active entities
     */
    long countByDeletedFalse();

    /**
     * Count deleted entities using method name derivation.
     * This bypasses the @Where clause filtering.
     * 
     * @return count of deleted entities
     */
    long countByDeletedTrue();

    /**
     * Permanently delete entities that have been soft-deleted before a certain date.
     * This is useful for cleanup operations.
     * 
     * @param cutoffDate the cutoff date for permanent deletion
     * @return number of permanently deleted entities
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} e WHERE e.deleted = true AND e.lastModifiedDate < :cutoffDate")
    int permanentlyDeleteOldSoftDeleted(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}