package com.example.trekkingapp.media;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {

    private long maxFileSizeBytes = 10L * 1024 * 1024;
    private List<String> allowedContentTypes = new ArrayList<>(List.of("image/jpeg", "image/png", "image/webp"));

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes == null ? new ArrayList<>() : allowedContentTypes;
    }
}
