package com.example.trekkingapp.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryMediaStorageService implements MediaStorageService {

    private final Cloudinary cloudinary;
    private final ImageValidationService imageValidationService;
    private final CloudinaryProperties cloudinaryProperties;

    public CloudinaryMediaStorageService(
            Cloudinary cloudinary,
            ImageValidationService imageValidationService,
            CloudinaryProperties cloudinaryProperties
    ) {
        this.cloudinary = cloudinary;
        this.imageValidationService = imageValidationService;
        this.cloudinaryProperties = cloudinaryProperties;
    }

    @Override
    public UploadResult uploadImage(MultipartFile file, String folder) {
        ensureConfigured();
        imageValidationService.validateImage(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", sanitizeFolder(folder),
                            "resource_type", "image",
                            "unique_filename", true,
                            "use_filename", false,
                            "public_id", UUID.randomUUID().toString()
                    )
            );

            return new UploadResult(
                    asString(response.get("url")),
                    asString(response.get("secure_url")),
                    asString(response.get("public_id")),
                    asInteger(response.get("width")),
                    asInteger(response.get("height")),
                    asString(response.get("format")),
                    asLong(response.get("bytes"))
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to upload image to Cloudinary", exception);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        ensureConfigured();
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete image from Cloudinary", exception);
        }
    }

    private void ensureConfigured() {
        if (isBlank(cloudinaryProperties.getCloudName())
                || isBlank(cloudinaryProperties.getApiKey())
                || isBlank(cloudinaryProperties.getApiSecret())) {
            throw new IllegalStateException("Cloudinary is not configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "uploads";
        }

        String normalized = folder.trim().toLowerCase()
                .replace('\\', '/')
                .replaceAll("[^a-z0-9/_-]", "-")
                .replaceAll("/+", "/")
                .replaceAll("-{2,}", "-")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
        return normalized.isBlank() ? "uploads" : normalized;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
