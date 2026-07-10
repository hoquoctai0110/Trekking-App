package com.example.trekkingapp.media;

public record UploadResult(
        String url,
        String secureUrl,
        String publicId,
        Integer width,
        Integer height,
        String format,
        Long bytes
) {
}
