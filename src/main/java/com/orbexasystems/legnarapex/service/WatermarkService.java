package com.orbexasystems.legnarapex.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
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
                    .watermark(Positions.CENTER, scaledWatermark(1440), 0.45f)
                    .outputFormat("jpg")
                    .outputQuality(0.87)
                    .toOutputStream(out);
        }

        return out.toByteArray();
    }

    // Scale and rotate the watermark 45° for a diagonal center overlay
    private BufferedImage scaledWatermark(int targetWidth) {
        int targetHeight = (int) ((double) watermarkImage.getHeight() / watermarkImage.getWidth() * targetWidth);

        // Scale first
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gs = scaled.createGraphics();
        gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gs.drawImage(watermarkImage, 0, 0, targetWidth, targetHeight, null);
        gs.dispose();

        // Rotate 45° — canvas must be large enough to avoid clipping
        double angle = Math.toRadians(45);
        int diagonal = (int) Math.ceil(Math.sqrt(targetWidth * targetWidth + targetHeight * targetHeight));
        BufferedImage rotated = new BufferedImage(diagonal, diagonal, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = rotated.createGraphics();
        gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gr.translate((diagonal - targetWidth) / 2.0, (diagonal - targetHeight) / 2.0);
        gr.rotate(angle, targetWidth / 2.0, targetHeight / 2.0);
        gr.drawImage(scaled, 0, 0, null);
        gr.dispose();

        return rotated;
    }
}
