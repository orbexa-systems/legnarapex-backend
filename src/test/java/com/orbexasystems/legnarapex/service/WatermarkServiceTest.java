package com.orbexasystems.legnarapex.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class WatermarkServiceTest {

    private WatermarkService watermarkService;

    @BeforeEach
    void setUp() {
        watermarkService = new WatermarkService();
    }

    @Test
    void applyWatermark_returnsValidJpeg() throws Exception {
        byte[] result = watermarkService.applyWatermark(createJpeg(100, 100));

        assertThat(result).isNotEmpty();
        assertThat(ImageIO.read(new ByteArrayInputStream(result))).isNotNull();
    }

    @Test
    void applyWatermark_outputRespects2400pxLimit() throws Exception {
        byte[] result = watermarkService.applyWatermark(createJpeg(3000, 2000));

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(2400);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(2400);
    }

    @Test
    void applyWatermark_smallImage_doesNotUpscale() throws Exception {
        byte[] result = watermarkService.applyWatermark(createJpeg(100, 80));

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(2400);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(2400);
    }

    private static byte[] createJpeg(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", out);
        return out.toByteArray();
    }
}
