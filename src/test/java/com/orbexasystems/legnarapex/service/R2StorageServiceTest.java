package com.orbexasystems.legnarapex.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class R2StorageServiceTest {

    @Mock private S3Client s3Client;

    @InjectMocks
    private R2StorageService r2StorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(r2StorageService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(r2StorageService, "publicUrl", "https://cdn.example.com");
    }

    @Test
    void upload_putsObjectWithCorrectMetadata() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        r2StorageService.upload("fotos/test.jpg", new byte[]{1, 2, 3});

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo("fotos/test.jpg");
        assertThat(captor.getValue().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void upload_returnsPublicUrl() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String url = r2StorageService.upload("fotos/test.jpg", new byte[]{1, 2, 3});

        assertThat(url).isEqualTo("https://cdn.example.com/fotos/test.jpg");
    }

    @Test
    void delete_callsDeleteObjectWithCorrectKey() {
        r2StorageService.delete("fotos/test.jpg");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo("fotos/test.jpg");
    }

    @Test
    void delete_whenS3Throws_doesNotPropagateException() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("S3 connection error"));

        // should not throw
        r2StorageService.delete("fotos/nonexistent.jpg");
    }
}
