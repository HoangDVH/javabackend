package com.hoang.jwtjava.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hoang.jwtjava.config.CloudinaryProperties;
import com.hoang.jwtjava.config.StorageProperties;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Upload multipart lên Cloudinary, trả về {@code secure_url} để lưu DB / trả API.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.cloudinary", name = "enabled", havingValue = "true")
public class CloudinaryUploadService {

    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final CloudinaryProperties cloudinaryProperties;
    private final StorageProperties storageProperties;

    private Cloudinary cloudinary;

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(cloudinaryProperties.getCloudName())
                || !StringUtils.hasText(cloudinaryProperties.getApiKey())
                || !StringUtils.hasText(cloudinaryProperties.getApiSecret()))
            throw new IllegalStateException(
                    "app.cloudinary.enabled=true nhưng thiếu cloud-name / api-key / api-secret.");
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryProperties.getCloudName(),
                "api_key", cloudinaryProperties.getApiKey(),
                "api_secret", cloudinaryProperties.getApiSecret()));
    }

    public List<String> uploadProductImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty())
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        List<String> urls = new ArrayList<>();
        boolean any = false;
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty())
                continue;
            any = true;
            urls.add(uploadOne(f));
        }
        if (!any)
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        return urls;
    }

    private String uploadOne(MultipartFile file) {
        if (file.getSize() > storageProperties.getMaxBytes())
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);

        String contentType = "";
        if (StringUtils.hasText(file.getContentType()))
            contentType = file.getContentType().split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (!MIME_TO_EXT.containsKey(contentType))
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        }
        if (bytes.length > storageProperties.getMaxBytes())
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);

        String publicId = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("public_id", publicId);
        String folder = blankToNullOrTrim(cloudinaryProperties.getFolder());
        if (StringUtils.hasText(folder))
            options.put("folder", folder);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(bytes, options);
            String secureUrl = result != null ? (String) result.get("secure_url") : null;
            if (!StringUtils.hasText(secureUrl))
                throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
            return secureUrl;
        } catch (IOException e) {
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        }
    }

    private static String blankToNullOrTrim(String s) {
        if (!StringUtils.hasText(s))
            return null;
        return s.trim().replaceAll("/+$", "");
    }
}
