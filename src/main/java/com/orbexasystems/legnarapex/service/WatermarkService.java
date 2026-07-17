package com.orbexasystems.legnarapex.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Slf4j
@Service
public class WatermarkService {

    private BufferedImage watermarkImage;

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/watermark/legnarapex.png")) {
            if (is == null) {
                log.warn("Marca de agua no encontrada en /watermark/legnarapex.png — se procesarán fotos sin marca de agua");
                return;
            }
            watermarkImage = ImageIO.read(is);
            log.info("Marca de agua cargada OK");
        } catch (Exception e) {
            log.error("Error cargando marca de agua", e);
        }
    }

    /**
     * Aplica la marca de agua Legnarapex al JPG recibido.
     * Redimensiona a máximo 2400px en el lado más largo.
     * Si no hay imagen de marca de agua configurada, devuelve el JPG original.
     *
     * @param jpegBytes  bytes del JPG original
     * @return           bytes del JPG procesado con marca de agua
     */
    public byte[] aplicarMarcaDeAgua(byte[] jpegBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (watermarkImage == null) {
            // Sin marca de agua: solo redimensionar
            Thumbnails.of(new ByteArrayInputStream(jpegBytes))
                    .size(2400, 2400)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.87)
                    .toOutputStream(out);
        } else {
            Thumbnails.of(new ByteArrayInputStream(jpegBytes))
                    .size(2400, 2400)
                    .keepAspectRatio(true)
                    .watermark(Positions.BOTTOM_RIGHT, watermarkImage, 0.55f)
                    .outputFormat("jpg")
                    .outputQuality(0.87)
                    .toOutputStream(out);
        }

        return out.toByteArray();
    }
}
