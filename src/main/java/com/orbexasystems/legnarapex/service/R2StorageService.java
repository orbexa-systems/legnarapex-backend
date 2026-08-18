package com.orbexasystems.legnarapex.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;
import java.util.stream.Collectors;

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
                        .cacheControl("public, max-age=604800, immutable")
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

    public void deleteBatch(List<String> objectKeys) {
        if (objectKeys.isEmpty()) return;
        for (int i = 0; i < objectKeys.size(); i += 1000) {
            List<String> chunk = objectKeys.subList(i, Math.min(i + 1000, objectKeys.size()));
            List<ObjectIdentifier> identifiers = chunk.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .collect(Collectors.toList());
            try {
                s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder().objects(identifiers).build())
                        .build());
                log.info("Bulk deleted {} object(s) from R2", chunk.size());
            } catch (Exception e) {
                log.warn("Bulk delete failed for chunk of {}: {}", chunk.size(), e.getMessage());
            }
        }
    }
}
