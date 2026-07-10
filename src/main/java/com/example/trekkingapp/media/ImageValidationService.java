package com.example.trekkingapp.media;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ImageValidationService {

    private static final List<String> SUPPORTED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final MediaProperties mediaProperties;

    public ImageValidationService(MediaProperties mediaProperties) {
        this.mediaProperties = mediaProperties;
    }

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be empty");
        }

        String contentType = normalizeContentType(file.getContentType());
        Set<String> allowedContentTypes = resolveAllowedContentTypes();
        if (!allowedContentTypes.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image type. Allowed types: image/jpeg, image/png, image/webp");
        }

        if (file.getSize() > mediaProperties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("Image file exceeds the maximum size of 10 MB");
        }

        try {
            byte[] bytes = file.getBytes();
            if (!matchesSignature(bytes, contentType)) {
                throw new IllegalArgumentException("Image file content does not match its declared MIME type");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read image file", exception);
        }
    }

    private Set<String> resolveAllowedContentTypes() {
        Set<String> allowed = new HashSet<>();
        for (String contentType : mediaProperties.getAllowedContentTypes()) {
            if (contentType != null && !contentType.isBlank()) {
                allowed.add(normalizeContentType(contentType));
            }
        }
        if (allowed.isEmpty()) {
            allowed.addAll(SUPPORTED_TYPES);
        }
        return allowed;
    }

    private String normalizeContentType(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSignature(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0D
                    && bytes[5] == 0x0A
                    && bytes[6] == 0x1A
                    && bytes[7] == 0x0A;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 0x52
                    && bytes[1] == 0x49
                    && bytes[2] == 0x46
                    && bytes[3] == 0x46
                    && bytes[8] == 0x57
                    && bytes[9] == 0x45
                    && bytes[10] == 0x42
                    && bytes[11] == 0x50;
            default -> false;
        };
    }
}
