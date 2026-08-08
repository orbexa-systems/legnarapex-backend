package com.orbexasystems.legnarapex.controller;

import com.orbexasystems.legnarapex.model.Foto;
import com.orbexasystems.legnarapex.service.FotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FotoController.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:3000")
class FotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FotoService fotoService;

    @Test
    void upload_validFile_returns201WithFoto() throws Exception {
        UUID locationId = UUID.randomUUID();
        Foto foto = Foto.builder()
                .id(UUID.randomUUID())
                .codigo("2V0A9780")
                .lugarId(locationId)
                .fechaFoto(LocalDate.now())
                .horaFoto(LocalTime.now())
                .urlFoto("https://cdn.example.com/fotos/test.jpg")
                .fechaSubida(OffsetDateTime.now())
                .expiraEn(OffsetDateTime.now().plusDays(8))
                .build();

        when(fotoService.processAndUploadPhoto(any(), any())).thenReturn(foto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "2V0A9780.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/fotos/upload")
                        .file(file)
                        .param("lugar_id", locationId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("2V0A9780"));
    }

    @Test
    void upload_serviceThrowsBadRequest_returns400() throws Exception {
        when(fotoService.processAndUploadPhoto(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]
        );

        mockMvc.perform(multipart("/fotos/upload")
                        .file(file)
                        .param("lugar_id", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_existingFoto_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(fotoService).deletePhoto(id);

        mockMvc.perform(delete("/fotos/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistentFoto_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"))
                .when(fotoService).deletePhoto(id);

        mockMvc.perform(delete("/fotos/" + id))
                .andExpect(status().isNotFound());
    }
}
