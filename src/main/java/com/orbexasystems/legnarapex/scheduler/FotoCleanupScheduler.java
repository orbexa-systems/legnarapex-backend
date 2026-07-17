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

    /**
     * Cron de limpieza nocturna — corre cada día a las 2:00 AM (hora del servidor, UTC).
     * Zona horaria México: 2:00 AM CST = 8:00 AM UTC.
     *
     * Elimina de Cloudflare R2 y de PostgreSQL todas las fotos expiradas.
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    public void limpiarFotosExpiradas() {
        log.info("Iniciando limpieza de fotos expiradas...");
        try {
            List<Foto> eliminadas = fotoService.eliminarExpiradas();
            log.info("Limpieza completada — {} foto(s) eliminada(s)", eliminadas.size());
        } catch (Exception e) {
            log.error("Error en limpieza de fotos expiradas", e);
        }
    }
}
