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

    // 2:00 AM Mexico City (CST/UTC-6) = 8:00 AM UTC
    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    public void cleanUpExpiredPhotos() {
        log.info("Starting expired photo cleanup...");
        try {
            List<Foto> deleted = fotoService.deleteExpiredPhotos();
            log.info("Cleanup completed — {} photo(s) deleted", deleted.size());
        } catch (Exception e) {
            log.error("Error during expired photo cleanup", e);
        }
    }
}
