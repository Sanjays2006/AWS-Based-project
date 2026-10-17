package com.cloudidentity.util;

import com.cloudidentity.dto.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public final class PaginationUtil {

    private PaginationUtil() {}

    /**
     * Creates a Pageable object with standard page/size parameters and safety defaults.
     */
    public static Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        // Cap max size to protect database from abuse
        int limit = Math.min(Math.max(size, 1), 100);
        int pageIndex = Math.max(page, 0);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "DESC"), 
                            sortBy != null ? sortBy : "id");
        
        return PageRequest.of(pageIndex, limit, sort);
    }

    /**
     * Converts a Spring Page object into our standard PagedResponse wrapper.
     */
    public static <T, R> PagedResponse<R> toPagedResponse(Page<T> page, List<R> mappedContent) {
        return PagedResponse.<R>builder()
                .content(mappedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
