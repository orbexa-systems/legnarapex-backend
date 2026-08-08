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
                log.warn("Watermark not found at /watermark/legnarapex.png — photos will be processed without watermark");
                return;
            }
            watermarkImage = ImageIO.read(is);
            log.info("Watermark loaded OK");
        } catch (Exception e) {
            log.error("Error loading watermark", e);
        }
    }

    public byte[] applyWatermark(byte[] jpegBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (watermarkImage == null) {
            // no watermark configured: resize only
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
