package com.example.trekkingapp.admin.dto.response;

public record AdminCheckInSummaryResponse(
        long totalCheckIns,
        long successfulCheckIns,
        long failedCheckIns
) {
}
