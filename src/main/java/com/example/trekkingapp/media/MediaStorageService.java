package com.example.trekkingapp.media;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    UploadResult uploadImage(MultipartFile file, String folder);

    void deleteImage(String publicId);
}
