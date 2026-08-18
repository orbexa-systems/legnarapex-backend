package com.orbexasystems.legnarapex.scheduler;

import com.orbexasystems.legnarapex.model.Foto;
import com.orbexasystems.legnarapex.service.FotoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FotoCleanupSchedulerTest {

    @Mock private FotoService fotoService;

    @InjectMocks
    private FotoCleanupScheduler fotoCleanupScheduler;

    @Test
    void cleanUpAllPhotos_callsDeleteAllPhotos() {
        when(fotoService.deleteAllPhotos()).thenReturn(List.of(
                Foto.builder().id(UUID.randomUUID()).code("EXP1").build()
        ));

        fotoCleanupScheduler.cleanUpAllPhotos();

        verify(fotoService).deleteAllPhotos();
    }

    @Test
    void cleanUpAllPhotos_serviceThrows_doesNotPropagateException() {
        when(fotoService.deleteAllPhotos()).thenThrow(new RuntimeException("DB connection lost"));

        // should not throw — scheduler must not crash on cleanup failure
        fotoCleanupScheduler.cleanUpAllPhotos();

        verify(fotoService).deleteAllPhotos();
    }

    @Test
    void cleanUpAllPhotos_noPhotos_completes() {
        when(fotoService.deleteAllPhotos()).thenReturn(List.of());

        fotoCleanupScheduler.cleanUpAllPhotos();

        verify(fotoService).deleteAllPhotos();
    }
}
