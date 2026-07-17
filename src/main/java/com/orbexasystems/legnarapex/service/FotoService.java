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

    private static final ZoneId ZONA_MX = ZoneId.of("America/Mexico_City");
    private static final int DIAS_EXPIRACION = 8;

    private final FotoRepository fotoRepository;
    private final WatermarkService watermarkService;
    private final R2StorageService r2StorageService;

    /**
     * Flujo completo de subida:
     * 1. Extrae código del nombre de archivo
     * 2. Lee EXIF (fecha y hora de la toma)
     * 3. Aplica marca de agua con Thumbnailator
     * 4. Sube JPG resultante a Cloudflare R2
     * 5. Persiste el registro en PostgreSQL
     */
    public Foto procesarYSubirFoto(UUID lugarId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }

        try {
            byte[] bytes = file.getBytes();
            String codigo = extraerCodigo(file.getOriginalFilename());
            LocalDateTime fechaHora = extraerFechaHoraExif(bytes);

            byte[] procesado = watermarkService.aplicarMarcaDeAgua(bytes);

            UUID id = UUID.randomUUID();
            String objectKey = "fotos/" + id + ".jpg";
            String urlFoto = r2StorageService.upload(objectKey, procesado);

            OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

            Foto foto = Foto.builder()
                    .id(id)
                    .codigo(codigo)
                    .lugarId(lugarId)
                    .fechaFoto(fechaHora.toLocalDate())
                    .horaFoto(fechaHora.toLocalTime())
                    .urlFoto(urlFoto)
                    .fechaSubida(ahora)
                    .expiraEn(ahora.plusDays(DIAS_EXPIRACION))
                    .build();

            return fotoRepository.save(foto);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error procesando foto {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error procesando la foto: " + e.getMessage());
        }
    }

    public void eliminarFoto(UUID id) {
        Foto foto = fotoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto no encontrada"));

        r2StorageService.delete("fotos/" + id + ".jpg");
        fotoRepository.delete(foto);
        log.info("Foto eliminada manualmente: {}", foto.getCodigo());
    }

    public List<Foto> eliminarExpiradas() {
        List<Foto> expiradas = fotoRepository.findByExpiraEnBefore(OffsetDateTime.now(ZoneOffset.UTC));
        for (Foto foto : expiradas) {
            r2StorageService.delete("fotos/" + foto.getId() + ".jpg");
            fotoRepository.delete(foto);
        }
        log.info("Fotos expiradas eliminadas en limpieza: {}", expiradas.size());
        return expiradas;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extraerCodigo(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return "SIN_CODIGO";
        int punto = nombreArchivo.lastIndexOf('.');
        return (punto > 0 ? nombreArchivo.substring(0, punto) : nombreArchivo).toUpperCase();
    }

    private LocalDateTime extraerFechaHoraExif(byte[] jpegBytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(jpegBytes));
            ExifSubIFDDirectory dir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (dir != null) {
                Date fecha = dir.getDateOriginal(TimeZone.getTimeZone(ZONA_MX));
                if (fecha != null) {
                    return fecha.toInstant().atZone(ZONA_MX).toLocalDateTime();
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo leer EXIF, usando fecha actual: {}", e.getMessage());
        }
        return LocalDateTime.now(ZONA_MX);
    }
}
