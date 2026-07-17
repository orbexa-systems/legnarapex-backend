package com.orbexasystems.legnarapex.controller;

import com.orbexasystems.legnarapex.model.Foto;
import com.orbexasystems.legnarapex.service.FotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/fotos")
@RequiredArgsConstructor
public class FotoController {

    private final FotoService fotoService;

    /**
     * Recibe un JPG, lo procesa (EXIF + marca de agua) y lo almacena en R2 + DB.
     *
     * Request: multipart/form-data
     *   lugar_id  UUID  — ID del lugar de la sesión
     *   file      File  — archivo JPG
     *
     * Response: 201 Created con el registro de la foto
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Foto> upload(
            @RequestParam("lugar_id") UUID lugarId,
            @RequestParam("file") MultipartFile file
    ) {
        Foto foto = fotoService.procesarYSubirFoto(lugarId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(foto);
    }

    /**
     * Elimina una foto de R2 y de la base de datos.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fotoService.eliminarFoto(id);
        return ResponseEntity.noContent().build();
    }
}
