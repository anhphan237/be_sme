package com.sme.be_sme.modules.content.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryUploadService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryUploadService.class);

    private final Cloudinary cloudinary;

    /**
     * Upload file to Cloudinary.
     *
     * @param file the multipart file to upload
     * @return secure URL of the uploaded file, or null on failure
     */
    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        File tempFile = Files.createTempFile("doc-upload-", "-" + getSafeFileName(file.getOriginalFilename())).toFile();
        try {
            file.transferTo(tempFile);
            Map<?, ?> params = ObjectUtils.asMap("resource_type", "auto");
            Map<?, ?> result = cloudinary.uploader().upload(tempFile, params);
            String url = (String) result.get("secure_url");
            log.info("Uploaded to Cloudinary: {}", url);
            return url;
        } finally {
            tempFile.delete();
        }
    }

    private static String getSafeFileName(String name) {
        if (name == null || name.isBlank()) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
