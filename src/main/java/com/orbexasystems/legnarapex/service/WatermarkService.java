package com.orbexasystems.legnarapex.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Service
public class WatermarkService {

    private static final int    MAX_PX      = 2400;
    private static final float  JPEG_QUALITY = 0.87f;
    private static final double ANGLE_DEG   = -25.0;
    // Legnar red #C0392B at ~22% opacity
    private static final Color  WM_COLOR    = new Color(192, 57, 43, 56);

    public byte[] applyWatermark(byte[] jpegBytes) throws Exception {
        BufferedImage img = Thumbnails.of(new ByteArrayInputStream(jpegBytes))
                .size(MAX_PX, MAX_PX)
                .keepAspectRatio(true)
                .asBufferedImage();

        BufferedImage rgb = toRgb(img);
        paintTiledWatermark(rgb);
        return encodeJpeg(rgb);
    }

    private void paintTiledWatermark(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        g.setColor(WM_COLOR);

        // Font size proportional to image width — same visual density regardless of resolution
        int fontSize = w / 14;
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics fm = g.getFontMetrics();

        int line1W = fm.stringWidth("LEGNAR");
        int line2W = fm.stringWidth("APEX");
        int tileW  = Math.max(line1W, line2W) + fontSize;
        int tileH  = fm.getHeight() * 2 + fontSize / 2;

        // Rotate around image center then tile, covering the full diagonal
        g.rotate(Math.toRadians(ANGLE_DEG), w / 2.0, h / 2.0);
        int ext = (int) Math.ceil(Math.sqrt((double) w * w + (double) h * h));

        for (int y = h / 2 - ext; y < h / 2 + ext; y += tileH) {
            for (int x = w / 2 - ext; x < w / 2 + ext; x += tileW) {
                g.drawString("LEGNAR", x, y);
                g.drawString("APEX",   x + (line1W - line2W) / 2, y + fm.getHeight());
            }
        }

        g.dispose();
    }

    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static byte[] encodeJpeg(BufferedImage img) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        writer.setOutput(new MemoryCacheImageOutputStream(out));
        writer.write(null, new IIOImage(img, null, null), param);
        writer.dispose();
        return out.toByteArray();
    }
}
