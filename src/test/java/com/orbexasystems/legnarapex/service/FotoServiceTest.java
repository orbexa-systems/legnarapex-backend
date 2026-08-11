package com.orbexasystems.legnarapex.service;

import com.orbexasystems.legnarapex.model.Foto;
import com.orbexasystems.legnarapex.repository.FotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FotoServiceTest {

    @Mock private FotoRepository fotoRepository;
    @Mock private WatermarkService watermarkService;
    @Mock private R2StorageService r2StorageService;

    @InjectMocks
    private FotoService fotoService;

    @Test
    void processAndUploadPhoto_emptyFile_throwsBadRequest() {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> fotoService.processAndUploadPhoto(UUID.randomUUID(), empty))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void processAndUploadPhoto_validJpeg_savesCorrectCodeAndLocation() throws Exception {
        UUID locationId = UUID.randomUUID();
        byte[] jpegBytes = createMinimalJpeg();
        MockMultipartFile file = new MockMultipartFile("file", "2V0A9780.jpg", "image/jpeg", jpegBytes);

        when(watermarkService.applyWatermark(any())).thenReturn(jpegBytes);
        when(r2StorageService.upload(anyString(), any())).thenReturn("https://cdn.example.com/photos/id.jpg");
        when(fotoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fotoService.processAndUploadPhoto(locationId, file);

        ArgumentCaptor<Foto> captor = ArgumentCaptor.forClass(Foto.class);
        verify(fotoRepository).save(captor.capture());

        assertThat(captor.getValue().getCode()).isEqualTo("2V0A9780");
        assertThat(captor.getValue().getLocationId()).isEqualTo(locationId);
        assertThat(captor.getValue().getExpiresAt()).isAfter(captor.getValue().getUploadedAt());
    }

    @Test
    void processAndUploadPhoto_validJpeg_appliesWatermarkAndUploadsToR2() throws Exception {
        byte[] jpegBytes = createMinimalJpeg();
        MockMultipartFile file = new MockMultipartFile("file", "IMG001.jpg", "image/jpeg", jpegBytes);

        when(watermarkService.applyWatermark(jpegBytes)).thenReturn(jpegBytes);
        when(r2StorageService.upload(anyString(), any())).thenReturn("https://cdn.example.com/photos/id.jpg");
        when(fotoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fotoService.processAndUploadPhoto(UUID.randomUUID(), file);

        verify(watermarkService).applyWatermark(jpegBytes);
        verify(r2StorageService).upload(argThat(k -> k.startsWith("photos/") && k.endsWith(".jpg")), eq(jpegBytes));
    }

    @Test
    void processAndUploadPhoto_duplicateCode_returnsExistingWithoutReprocessing() throws Exception {
        byte[] jpegBytes = createMinimalJpeg();
        MockMultipartFile file = new MockMultipartFile("file", "2V0A9780.jpg", "image/jpeg", jpegBytes);
        Foto existing = Foto.builder().id(UUID.randomUUID()).code("2V0A9780").build();
        when(fotoRepository.findByCode("2V0A9780")).thenReturn(Optional.of(existing));

        Foto result = fotoService.processAndUploadPhoto(UUID.randomUUID(), file);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(watermarkService, r2StorageService);
        verify(fotoRepository, never()).save(any());
    }

    @Test
    void deletePhoto_notFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(fotoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fotoService.deletePhoto(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletePhoto_found_deletesFromR2AndRepository() {
        UUID id = UUID.randomUUID();
        Foto foto = Foto.builder().id(id).code("TEST").build();
        when(fotoRepository.findById(id)).thenReturn(Optional.of(foto));

        fotoService.deletePhoto(id);

        verify(r2StorageService).delete("photos/" + id + ".jpg");
        verify(fotoRepository).delete(foto);
    }

    @Test
    void deleteExpiredPhotos_deletesEachFromR2AndRepository() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<Foto> expired = List.of(
                Foto.builder().id(id1).code("EXP1").build(),
                Foto.builder().id(id2).code("EXP2").build()
        );
        when(fotoRepository.findByExpiresAtBefore(any())).thenReturn(expired);

        List<Foto> result = fotoService.deleteExpiredPhotos();

        assertThat(result).hasSize(2);
        verify(r2StorageService).delete("photos/" + id1 + ".jpg");
        verify(r2StorageService).delete("photos/" + id2 + ".jpg");
        verify(fotoRepository, times(2)).delete(any(Foto.class));
    }

    @Test
    void deleteExpiredPhotos_noExpiredPhotos_returnsEmptyList() {
        when(fotoRepository.findByExpiresAtBefore(any())).thenReturn(List.of());

        List<Foto> result = fotoService.deleteExpiredPhotos();

        assertThat(result).isEmpty();
        verifyNoInteractions(r2StorageService);
    }

    static byte[] createMinimalJpeg() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", out);
        return out.toByteArray();
    }
}
