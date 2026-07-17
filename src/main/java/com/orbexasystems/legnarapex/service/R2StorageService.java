package com.orbexasystems.legnarapex.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Slf4j
@Service
public class R2StorageService {

    @Value("${r2.account-id}")
    private String accountId;

    @Value("${r2.access-key-id}")
    private String accessKeyId;

    @Value("${r2.secret-access-key}")
    private String secretAccessKey;

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.public-url}")
    private String publicUrl;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        s3Client = S3Client.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build();
        log.info("R2StorageService inicializado — bucket: {}", bucketName);
    }

    /**
     * Sube un JPG a R2 y devuelve la URL pública.
     * @param objectKey  clave del objeto, ej: "fotos/uuid.jpg"
     * @param jpegBytes  contenido del archivo
     */
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
        log.info("Subido a R2: {}", url);
        return url;
    }

    /**
     * Elimina un objeto de R2. No lanza excepción si el objeto no existe.
     */
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build()
            );
            log.info("Eliminado de R2: {}", objectKey);
        } catch (Exception e) {
            log.warn("No se pudo eliminar de R2 ({}): {}", objectKey, e.getMessage());
        }
    }
}
