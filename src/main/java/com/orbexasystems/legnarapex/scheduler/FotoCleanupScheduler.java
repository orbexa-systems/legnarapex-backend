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

    // Every day at 3:00 AM Mexico City time
    @Scheduled(cron = "0 0 3 * * *", zone = "America/Mexico_City")
    public void cleanUpAllPhotos() {
        log.info("Starting daily photo cleanup...");
        try {
            List<Foto> deleted = fotoService.deleteAllPhotos();
            log.info("Daily cleanup completed — {} photo(s) deleted", deleted.size());
        } catch (Exception e) {
            log.error("Error during weekly photo cleanup", e);
        }
    }
}
