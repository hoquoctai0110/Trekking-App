package com.example.trekkingapp.common;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {
}
