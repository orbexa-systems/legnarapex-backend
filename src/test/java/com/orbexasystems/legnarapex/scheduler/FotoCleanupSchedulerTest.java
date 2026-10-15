package com.orbexasystems.legnarapex.scheduler;

import com.orbexasystems.legnarapex.model.Foto;
import com.orbexasystems.legnarapex.service.EmailNotificationService;
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
    @Mock private EmailNotificationService emailNotificationService;

    @InjectMocks
    private FotoCleanupScheduler fotoCleanupScheduler;

    @Test
    void cleanUpAllPhotos_sendsSuccessNotificationWithCount() {
        when(fotoService.deleteAllPhotos()).thenReturn(List.of(
                Foto.builder().id(UUID.randomUUID()).code("EXP1").build()
        ));

        fotoCleanupScheduler.cleanUpAllPhotos();

        verify(fotoService).deleteAllPhotos();
        verify(emailNotificationService).sendCleanupSuccess(1);
    }

    @Test
    void cleanUpAllPhotos_serviceThrows_sendsFailureNotification() {
        RuntimeException error = new RuntimeException("DB connection lost");
        when(fotoService.deleteAllPhotos()).thenThrow(error);

        // should not throw — scheduler must not crash on cleanup failure
        fotoCleanupScheduler.cleanUpAllPhotos();

        verify(emailNotificationService).sendCleanupFailure("DB connection lost");
        verify(emailNotificationService, never()).sendCleanupSuccess(anyInt());
    }

    @Test
    void cleanUpAllPhotos_noPhotos_sendsSuccessWithZeroCount() {
        when(fotoService.deleteAllPhotos()).thenReturn(List.of());

        fotoCleanupScheduler.cleanUpAllPhotos();

        verify(emailNotificationService).sendCleanupSuccess(0);
    }
}
