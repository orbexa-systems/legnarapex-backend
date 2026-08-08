package com.orbexasystems.legnarapex.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.orbexasystems.legnarapex.model.Foto;
import com.orbexasystems.legnarapex.repository.FotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FotoService {

    private static final ZoneId MX_ZONE = ZoneId.of("America/Mexico_City");
    private static final int EXPIRY_DAYS = 8;

    private final FotoRepository fotoRepository;
    private final WatermarkService watermarkService;
    private final R2StorageService r2StorageService;

    public Foto processAndUploadPhoto(UUID locationId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        try {
            byte[] bytes = file.getBytes();
            String code = extractCode(file.getOriginalFilename());
            LocalDateTime photoDateTime = extractExifDateTime(bytes);

            byte[] processed = watermarkService.applyWatermark(bytes);

            UUID id = UUID.randomUUID();
            String objectKey = "fotos/" + id + ".jpg";
            String photoUrl = r2StorageService.upload(objectKey, processed);

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            Foto foto = Foto.builder()
                    .id(id)
                    .codigo(code)
                    .lugarId(locationId)
                    .fechaFoto(photoDateTime.toLocalDate())
                    .horaFoto(photoDateTime.toLocalTime())
                    .urlFoto(photoUrl)
                    .fechaSubida(now)
                    .expiraEn(now.plusDays(EXPIRY_DAYS))
                    .build();

            return fotoRepository.save(foto);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing photo {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing photo: " + e.getMessage());
        }
    }

    public void deletePhoto(UUID id) {
        Foto foto = fotoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));

        r2StorageService.delete("fotos/" + id + ".jpg");
        fotoRepository.delete(foto);
        log.info("Photo deleted manually: {}", foto.getCodigo());
    }

    public List<Foto> deleteExpiredPhotos() {
        List<Foto> expired = fotoRepository.findByExpiraEnBefore(OffsetDateTime.now(ZoneOffset.UTC));
        for (Foto foto : expired) {
            r2StorageService.delete("fotos/" + foto.getId() + ".jpg");
            fotoRepository.delete(foto);
        }
        log.info("Expired photos deleted in cleanup: {}", expired.size());
        return expired;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String extractCode(String filename) {
        if (filename == null || filename.isBlank()) return "NO_CODE";
        int dot = filename.lastIndexOf('.');
        return (dot > 0 ? filename.substring(0, dot) : filename).toUpperCase();
    }

    private LocalDateTime extractExifDateTime(byte[] jpegBytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(jpegBytes));
            ExifSubIFDDirectory dir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (dir != null) {
                Date date = dir.getDateOriginal(TimeZone.getTimeZone(MX_ZONE));
                if (date != null) {
                    return date.toInstant().atZone(MX_ZONE).toLocalDateTime();
                }
            }
        } catch (Exception e) {
            log.warn("Could not read EXIF, using current date/time: {}", e.getMessage());
        }
        return LocalDateTime.now(MX_ZONE);
    }
}
