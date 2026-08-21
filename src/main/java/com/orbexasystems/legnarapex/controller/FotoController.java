package com.orbexasystems.legnarapex.controller;

import com.orbexasystems.legnarapex.model.Foto;
import com.orbexasystems.legnarapex.service.FotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/fotos")
@RequiredArgsConstructor
public class FotoController {

    private final FotoService fotoService;

    @Value("${internal.api.key:}")
    private String internalApiKey;

    private void requireInternalKey(String key) {
        if (internalApiKey.isBlank() || !internalApiKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Foto> upload(
            @RequestParam("lugar_id") UUID locationId,
            @RequestParam("file") MultipartFile file
    ) {
        Foto foto = fotoService.processAndUploadPhoto(locationId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(foto);
    }

    @PostMapping(value = "/admin/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Foto> adminUpload(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @RequestParam("lugar_id") UUID locationId,
            @RequestParam("file") MultipartFile file
    ) {
        requireInternalKey(key);
        Foto foto = fotoService.processAndUploadPhotoWithWatermark(locationId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(foto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @PathVariable UUID id
    ) {
        requireInternalKey(key);
        fotoService.deletePhoto(id);
        return ResponseEntity.noContent().build();
    }
}
