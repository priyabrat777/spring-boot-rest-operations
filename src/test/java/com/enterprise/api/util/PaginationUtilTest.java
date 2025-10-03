package com.enterprise.api.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaginationUtil.
 * Tests all utility methods with comprehensive coverage including edge cases.
 */
class PaginationUtilTest {

    @Test
    void createPageable_WithValidParameters_ShouldReturnPageable() {
        // Given
        Integer page = 1;
        Integer size = 10;

        // When
        Pageable result = PaginationUtil.createPageable(page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertTrue(result.getSort().isUnsorted());
    }

    @Test
    void createPageable_WithNullPage_ShouldUseDefaultPage() {
        // Given
        Integer page = null;
        Integer size = 10;

        // When
        Pageable result = PaginationUtil.createPageable(page, size);

        // Then
        assertNotNull(result);
        assertEquals(PaginationUtil.DEFAULT_PAGE_NUMBER, result.getPageNumber());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void createPageable_WithNegativePage_ShouldUseDefaultPage() {
        // Given
        Integer page = -1;
        Integer size = 10;

        // When
        Pageable result = PaginationUtil.createPageable(page, size);

        // Then
        assertNotNull(result);
        assertEquals(PaginationUtil.DEFAULT_PAGE_NUMBER, result.getPageNumber());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void createPageable_WithNullSize_ShouldUseDefaultSize() {
        // Given
        Integer page = 0;
        Integer size = null;

        // When
        Pageable result = PaginationUtil.createPageable(page, size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getPageNumber());
        assertEquals(PaginationUtil.DEFAULT_PAGE_SIZE, result.getPageSize());
    }

    @Test
    void createPageable_WithZeroSize_ShouldUseDefaultSize() {
        // Given
        Integer page = 0;
        Integer size = 0;

        // When
        Pageable result = PaginationUtil.createPageable(page, size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getPageNumber());
        assertEquals(PaginationUtil.DEFAULT_PAGE_SIZE, result.getPageSize());
    }

    @Test
    void createPageable_WithNegativeSize_ShouldUseDefaultSize() {
        // Given
        Integer page = 0;
        Integer size = -5;

        // When
        Pageable result = PaginationUtil.createPageable(page, size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getPageNumber());
        assertEquals(PaginationUtil.DEFAULT_PAGE_SIZE, result.getPageSize());
    }

    @Test
    void createPageable_WithOversizedPage_ShouldCapAtMaxSize() {
        // Given
        Integer page = 0;
        Integer size = 2000; // Exceeds MAX_PAGE_SIZE

        // When
        Pageable result = PaginationUtil.createPageable(page, size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getPageNumber());
        assertEquals(PaginationUtil.MAX_PAGE_SIZE, result.getPageSize());
    }

    @Test
    void createPageable_WithSort_ShouldReturnPageableWithSort() {
        // Given
        Integer page = 1;
        Integer size = 10;
        Sort sort = Sort.by(Sort.Direction.DESC, "name");

        // When
        Pageable result = PaginationUtil.createPageable(page, size, sort);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertFalse(result.getSort().isUnsorted());
        assertEquals(Sort.Direction.DESC, result.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void createPageable_WithNullSort_ShouldReturnUnsortedPageable() {
        // Given
        Integer page = 1;
        Integer size = 10;
        Sort sort = null;

        // When
        Pageable result = PaginationUtil.createPageable(page, size, sort);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertTrue(result.getSort().isUnsorted());
    }

    @Test
    void createPageable_WithSortByAndDirection_ShouldReturnSortedPageable() {
        // Given
        Integer page = 1;
        Integer size = 10;
        String sortBy = "name";
        String sortDirection = "DESC";

        // When
        Pageable result = PaginationUtil.createPageable(page, size, sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertFalse(result.getSort().isUnsorted());
        assertEquals(Sort.Direction.DESC, result.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void createPageable_WithNullSortBy_ShouldReturnUnsortedPageable() {
        // Given
        Integer page = 1;
        Integer size = 10;
        String sortBy = null;
        String sortDirection = "DESC";

        // When
        Pageable result = PaginationUtil.createPageable(page, size, sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertTrue(result.getSort().isUnsorted());
    }

    @Test
    void createPageable_WithEmptySortBy_ShouldReturnUnsortedPageable() {
        // Given
        Integer page = 1;
        Integer size = 10;
        String sortBy = "   ";
        String sortDirection = "DESC";

        // When
        Pageable result = PaginationUtil.createPageable(page, size, sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertTrue(result.getSort().isUnsorted());
    }

    @Test
    void createPageable_WithInvalidSortDirection_ShouldDefaultToASC() {
        // Given
        Integer page = 1;
        Integer size = 10;
        String sortBy = "name";
        String sortDirection = "INVALID";

        // When
        Pageable result = PaginationUtil.createPageable(page, size, sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertFalse(result.getSort().isUnsorted());
        assertEquals(Sort.Direction.ASC, result.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void createCursorPageable_WithValidSize_ShouldReturnPageable() {
        // Given
        Integer size = 15;

        // When
        Pageable result = PaginationUtil.createCursorPageable(size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getPageNumber());
        assertEquals(15, result.getPageSize());
    }

    @Test
    void createCursorPageable_WithNullSize_ShouldUseDefaultSize() {
        // Given
        Integer size = null;

        // When
        Pageable result = PaginationUtil.createCursorPageable(size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getPageNumber());
        assertEquals(PaginationUtil.DEFAULT_PAGE_SIZE, result.getPageSize());
    }

    @Test
    void createOptimizedPageable_WithValidParameters_ShouldReturnOptimizedPageable() {
        // Given
        Integer page = 1;
        Integer size = 50;
        String sortBy = "createdDate";

        // When
        Pageable result = PaginationUtil.createOptimizedPageable(page, size, sortBy);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(50, result.getPageSize());
        assertFalse(result.getSort().isUnsorted());
        assertEquals(Sort.Direction.ASC, result.getSort().getOrderFor("createdDate").getDirection());
        assertEquals(Sort.Direction.ASC, result.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void createOptimizedPageable_WithLargeSize_ShouldCapAt100() {
        // Given
        Integer page = 1;
        Integer size = 200;
        String sortBy = "name";

        // When
        Pageable result = PaginationUtil.createOptimizedPageable(page, size, sortBy);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(100, result.getPageSize()); // Capped at 100
        assertFalse(result.getSort().isUnsorted());
    }

    @Test
    void createOptimizedPageable_WithNullSortBy_ShouldUseIdSort() {
        // Given
        Integer page = 1;
        Integer size = 50;
        String sortBy = null;

        // When
        Pageable result = PaginationUtil.createOptimizedPageable(page, size, sortBy);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(50, result.getPageSize());
        assertFalse(result.getSort().isUnsorted());
        assertEquals(Sort.Direction.ASC, result.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void isLargeDatasetRequest_WithLargePageSize_ShouldReturnTrue() {
        // Given
        Integer page = 0;
        Integer size = 150;

        // When
        boolean result = PaginationUtil.isLargeDatasetRequest(page, size);

        // Then
        assertTrue(result);
    }

    @Test
    void isLargeDatasetRequest_WithLargePageNumber_ShouldReturnTrue() {
        // Given
        Integer page = 150;
        Integer size = 20;

        // When
        boolean result = PaginationUtil.isLargeDatasetRequest(page, size);

        // Then
        assertTrue(result);
    }

    @Test
    void isLargeDatasetRequest_WithSmallParameters_ShouldReturnFalse() {
        // Given
        Integer page = 5;
        Integer size = 20;

        // When
        boolean result = PaginationUtil.isLargeDatasetRequest(page, size);

        // Then
        assertFalse(result);
    }

    @Test
    void isLargeDatasetRequest_WithNullParameters_ShouldReturnFalse() {
        // Given
        Integer page = null;
        Integer size = null;

        // When
        boolean result = PaginationUtil.isLargeDatasetRequest(page, size);

        // Then
        assertFalse(result);
    }

    @Test
    void createSort_WithValidParameters_ShouldReturnSort() {
        // Given
        String sortBy = "name";
        String sortDirection = "DESC";

        // When
        Sort result = PaginationUtil.createSort(sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertFalse(result.isUnsorted());
        assertEquals(Sort.Direction.DESC, result.getOrderFor("name").getDirection());
    }

    @Test
    void createSort_WithMultipleProperties_ShouldReturnMultiSort() {
        // Given
        String sortBy = "name,createdDate,id";
        String sortDirection = "ASC";

        // When
        Sort result = PaginationUtil.createSort(sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertFalse(result.isUnsorted());
        assertEquals(Sort.Direction.ASC, result.getOrderFor("name").getDirection());
        assertEquals(Sort.Direction.ASC, result.getOrderFor("createdDate").getDirection());
        assertEquals(Sort.Direction.ASC, result.getOrderFor("id").getDirection());
    }

    @Test
    void createSort_WithNullSortBy_ShouldReturnUnsorted() {
        // Given
        String sortBy = null;
        String sortDirection = "DESC";

        // When
        Sort result = PaginationUtil.createSort(sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertTrue(result.isUnsorted());
    }

    @Test
    void createSort_WithEmptySortBy_ShouldReturnUnsorted() {
        // Given
        String sortBy = "   ";
        String sortDirection = "DESC";

        // When
        Sort result = PaginationUtil.createSort(sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertTrue(result.isUnsorted());
    }

    @Test
    void createSort_WithNullDirection_ShouldDefaultToASC() {
        // Given
        String sortBy = "name";
        String sortDirection = null;

        // When
        Sort result = PaginationUtil.createSort(sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertFalse(result.isUnsorted());
        assertEquals(Sort.Direction.ASC, result.getOrderFor("name").getDirection());
    }

    @Test
    void createSort_WithPropertiesContainingSpaces_ShouldTrimProperties() {
        // Given
        String sortBy = " name , createdDate , id ";
        String sortDirection = "DESC";

        // When
        Sort result = PaginationUtil.createSort(sortBy, sortDirection);

        // Then
        assertNotNull(result);
        assertFalse(result.isUnsorted());
        assertEquals(Sort.Direction.DESC, result.getOrderFor("name").getDirection());
        assertEquals(Sort.Direction.DESC, result.getOrderFor("createdDate").getDirection());
        assertEquals(Sort.Direction.DESC, result.getOrderFor("id").getDirection());
    }

    @Test
    void constants_ShouldHaveExpectedValues() {
        // Then
        assertEquals(20, PaginationUtil.DEFAULT_PAGE_SIZE);
        assertEquals(1000, PaginationUtil.MAX_PAGE_SIZE);
        assertEquals(0, PaginationUtil.DEFAULT_PAGE_NUMBER);
    }
}