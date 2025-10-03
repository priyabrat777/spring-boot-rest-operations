package com.enterprise.api.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility class for pagination operations.
 * 
 * This utility provides:
 * - Safe pagination parameter handling
 * - Default pagination settings
 * - Sort parameter validation
 * - Performance-optimized pagination strategies
 * 
 * Requirements addressed:
 * - 5.3: Pagination support for large result sets
 * - 5.5: Query optimization with proper pagination
 */
public final class PaginationUtil {

    /**
     * Default page size for pagination.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum allowed page size to prevent performance issues.
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * Default page number (0-based).
     */
    public static final int DEFAULT_PAGE_NUMBER = 0;

    private PaginationUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a safe Pageable instance with validated parameters.
     * 
     * @param page page number (0-based)
     * @param size page size
     * @return validated Pageable instance
     */
    public static Pageable createPageable(Integer page, Integer size) {
        int validatedPage = validatePageNumber(page);
        int validatedSize = validatePageSize(size);
        return PageRequest.of(validatedPage, validatedSize);
    }

    /**
     * Creates a safe Pageable instance with sorting.
     * 
     * @param page page number (0-based)
     * @param size page size
     * @param sort sort specification
     * @return validated Pageable instance with sorting
     */
    public static Pageable createPageable(Integer page, Integer size, Sort sort) {
        int validatedPage = validatePageNumber(page);
        int validatedSize = validatePageSize(size);
        return PageRequest.of(validatedPage, validatedSize, sort != null ? sort : Sort.unsorted());
    }

    /**
     * Creates a safe Pageable instance with sorting by property.
     * 
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy property to sort by
     * @param sortDirection sort direction (ASC/DESC)
     * @return validated Pageable instance with sorting
     */
    public static Pageable createPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        int validatedPage = validatePageNumber(page);
        int validatedSize = validatePageSize(size);
        
        Sort sort = Sort.unsorted();
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if ("DESC".equalsIgnoreCase(sortDirection)) {
                direction = Sort.Direction.DESC;
            }
            sort = Sort.by(direction, sortBy);
        }
        
        return PageRequest.of(validatedPage, validatedSize, sort);
    }

    /**
     * Creates a Pageable for cursor-based pagination.
     * Only size is relevant for cursor-based pagination.
     * 
     * @param size page size
     * @return Pageable instance for cursor-based pagination
     */
    public static Pageable createCursorPageable(Integer size) {
        int validatedSize = validatePageSize(size);
        return PageRequest.of(0, validatedSize);
    }

    /**
     * Creates a Pageable optimized for large datasets.
     * Uses smaller page sizes and specific sorting for better performance.
     * 
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy property to sort by (should be indexed)
     * @return optimized Pageable instance
     */
    public static Pageable createOptimizedPageable(Integer page, Integer size, String sortBy) {
        int validatedPage = validatePageNumber(page);
        int validatedSize = Math.min(validatePageSize(size), 100); // Limit to 100 for large datasets
        
        Sort sort = Sort.by(Sort.Direction.ASC, "id"); // Default to ID sorting for consistency
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            sort = Sort.by(Sort.Direction.ASC, sortBy, "id"); // Add ID as secondary sort for consistency
        }
        
        return PageRequest.of(validatedPage, validatedSize, sort);
    }

    /**
     * Validates and normalizes page number.
     * 
     * @param page page number to validate
     * @return validated page number
     */
    private static int validatePageNumber(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE_NUMBER;
        }
        return page;
    }

    /**
     * Validates and normalizes page size.
     * 
     * @param size page size to validate
     * @return validated page size
     */
    private static int validatePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * Checks if pagination parameters indicate a request for large dataset handling.
     * 
     * @param page page number
     * @param size page size
     * @return true if this appears to be a large dataset request
     */
    public static boolean isLargeDatasetRequest(Integer page, Integer size) {
        int validatedPage = validatePageNumber(page);
        int validatedSize = validatePageSize(size);
        
        // Consider it a large dataset request if:
        // - Requesting a large page size
        // - Requesting a page far from the beginning
        return validatedSize > 100 || validatedPage > 100;
    }

    /**
     * Creates sort specification from string parameters.
     * Supports multiple sort properties separated by comma.
     * 
     * @param sortBy comma-separated list of properties to sort by
     * @param sortDirection sort direction (ASC/DESC)
     * @return Sort specification
     */
    public static Sort createSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return Sort.unsorted();
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if ("DESC".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.DESC;
        }

        String[] properties = sortBy.split(",");
        Sort sort = Sort.by(direction, properties[0].trim());
        
        for (int i = 1; i < properties.length; i++) {
            sort = sort.and(Sort.by(direction, properties[i].trim()));
        }
        
        return sort;
    }
}