package com.orbexasystems.legnarapex.scheduler;

import com.orbexasystems.legnarapex.model.Foto;
import com.orbexasystems.legnarapex.service.FotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FotoCleanupScheduler {

    private final FotoService fotoService;

    // Every Friday at 7:00 AM Mexico City time
    @Scheduled(cron = "0 0 7 * * FRI", zone = "America/Mexico_City")
    public void cleanUpAllPhotos() {
        log.info("Starting weekly photo cleanup...");
        try {
            List<Foto> deleted = fotoService.deleteAllPhotos();
            log.info("Weekly cleanup completed — {} photo(s) deleted", deleted.size());
        } catch (Exception e) {
            log.error("Error during weekly photo cleanup", e);
        }
    }
}
