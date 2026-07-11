package com.example.trekkingapp.admin;

import com.example.trekkingapp.common.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class AdminSortUtils {

    private AdminSortUtils() {
    }

    public static Pageable pageable(int page, int size, String sort, String defaultSortField, Set<String> allowedSortFields) {
        int normalizedPage = Math.max(page, AdminConstants.DEFAULT_PAGE);
        int normalizedSize = size <= 0 ? AdminConstants.DEFAULT_SIZE : Math.min(size, AdminConstants.MAX_PAGE_SIZE);
        Sort resolvedSort = parseSort(sort, defaultSortField, allowedSortFields);
        return PageRequest.of(normalizedPage, normalizedSize, resolvedSort);
    }

    public static Sort parseSort(String sort, String defaultSortField, Set<String> allowedSortFields) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, defaultSortField);
        }

        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        if (!allowedSortFields.contains(property)) {
            throw new ValidationException("Invalid sort field: " + property);
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && !parts[1].isBlank()) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException exception) {
                throw new ValidationException("Invalid sort direction: " + parts[1].trim());
            }
        }

        return Sort.by(direction, property);
    }
}
