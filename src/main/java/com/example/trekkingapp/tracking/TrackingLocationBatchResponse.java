package com.example.trekkingapp.tracking;

public record TrackingLocationBatchResponse(
        int syncedCount,
        int skippedCount
) {
}
