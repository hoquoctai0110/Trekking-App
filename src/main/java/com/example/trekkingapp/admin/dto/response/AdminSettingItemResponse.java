package com.example.trekkingapp.admin.dto.response;

public record AdminSettingItemResponse(
        String section,
        String key,
        String value,
        boolean secret,
        boolean configured
) {
}
