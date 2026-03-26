package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

@Service
public class ImageCropService {

    public File crop(File imageFile, int x, int y, int w, int h) {
        try {
            BufferedImage img = ImageIO.read(imageFile);

            BufferedImage cropped = img.getSubimage(x, y, w, h);

            File out = File.createTempFile("crop-", ".png");
            ImageIO.write(cropped, "png", out);

            return out;

        } catch (Exception e) {
            throw new RuntimeException("crop 실패", e);
        }
    }
}