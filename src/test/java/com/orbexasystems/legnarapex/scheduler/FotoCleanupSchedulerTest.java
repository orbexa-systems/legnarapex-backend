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
    void cleanUpExpiredPhotos_callsDeleteExpiredPhotos() {
        when(fotoService.deleteExpiredPhotos()).thenReturn(List.of(
                Foto.builder().id(UUID.randomUUID()).codigo("EXP1").build()
        ));

        fotoCleanupScheduler.cleanUpExpiredPhotos();

        verify(fotoService).deleteExpiredPhotos();
    }

    @Test
    void cleanUpExpiredPhotos_serviceThrows_doesNotPropagateException() {
        when(fotoService.deleteExpiredPhotos()).thenThrow(new RuntimeException("DB connection lost"));

        // should not throw — scheduler must not crash on cleanup failure
        fotoCleanupScheduler.cleanUpExpiredPhotos();

        verify(fotoService).deleteExpiredPhotos();
    }

    @Test
    void cleanUpExpiredPhotos_noExpiredPhotos_completes() {
        when(fotoService.deleteExpiredPhotos()).thenReturn(List.of());

        fotoCleanupScheduler.cleanUpExpiredPhotos();

        verify(fotoService).deleteExpiredPhotos();
    }
}
