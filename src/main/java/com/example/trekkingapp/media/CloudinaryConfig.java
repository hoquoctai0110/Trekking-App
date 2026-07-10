package com.example.trekkingapp.media;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", properties.getCloudName());
        config.put("api_key", properties.getApiKey());
        config.put("api_secret", properties.getApiSecret());
        config.put("secure", true);
        return new Cloudinary(config);
    }
}
