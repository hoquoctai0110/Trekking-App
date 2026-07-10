package com.example.trekkingapp.media;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageValidationServiceTest {

    private final ImageValidationService imageValidationService = new ImageValidationService(new MediaProperties());

    @Test
    void validateImageAcceptsSupportedJpeg() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "photo.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        assertDoesNotThrow(() -> imageValidationService.validateImage(file));
    }

    @Test
    void validateImageRejectsUnsupportedMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "photo.gif",
                "image/gif",
                new byte[]{0x47, 0x49, 0x46}
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> imageValidationService.validateImage(file)
        );

        assertEquals("Unsupported image type. Allowed types: image/jpeg, image/png, image/webp", exception.getMessage());
    }

    @Test
    void validateImageRejectsOversizedFile() {
        MediaProperties properties = new MediaProperties();
        properties.setMaxFileSizeBytes(3);
        ImageValidationService validationService = new ImageValidationService(properties);
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "photo.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validateImage(file)
        );

        assertEquals("Image file exceeds the maximum size of 10 MB", exception.getMessage());
    }
}
