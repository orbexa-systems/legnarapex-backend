package com.orbexasystems.legnarapex.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService {

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.public-url}")
    private String publicUrl;

    private final S3Client s3Client;

    public String upload(String objectKey, byte[] jpegBytes) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromBytes(jpegBytes)
        );
        String url = publicUrl.stripTrailing() + "/" + objectKey;
        log.info("Uploaded to R2: {}", url);
        return url;
    }

    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build()
            );
            log.info("Deleted from R2: {}", objectKey);
        } catch (Exception e) {
            log.warn("Could not delete from R2 ({}): {}", objectKey, e.getMessage());
        }
    }
}
