package dev.zawarudo.holo.modules.image;

import dev.zawarudo.holo.utils.ImageOperations;

import java.awt.image.BufferedImage;

public class AcheronFilter implements ImageFilter {

    private static final float RED_SAT_BOOST = 1.35f; // Make kept reds pop
    private static final float RED_VAL_BOOST = 1.10f; // Slightly brighter reds
    private static final float GRAY_DARKEN = 0.90f; // Darker grays for dramatic effect

    @Override
    public String name() {
        return "acheron";
    }

    @Override
    public String description() {
        return "Grayscale everything except strong reds";
    }

    @Override
    public BufferedImage apply(BufferedImage src, String[] args) throws IllegalArgumentException {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] px = ImageOperations.readPixels(src);

        for (int i = 0; i < px.length; i++) {
            int argb = px[i];

            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = argb & 0xFF;

            float[] hsv = ImageOperations.rgbToHsv(r, g, b);
            float hue = hsv[0];
            float sat = hsv[1];
            float val = hsv[2];

            if (isStrongRed(r, g, b, hue, sat, val)) {
                int rgb = ImageOperations.hsvToRgb(
                    hue,
                    ImageOperations.clamp01(sat * RED_SAT_BOOST),
                    ImageOperations.clamp01(val * RED_VAL_BOOST)
                );
                px[i] = (a << 24) | rgb;
            } else {
                int gray = ImageOperations.calculateGrayscaleValue(r, g, b);
                gray = (int) (gray * GRAY_DARKEN);
                gray = ImageOperations.clamp255(gray);
                px[i] = (a << 24) | (gray << 16) | (gray << 8) | gray;
            }
        }

        return ImageOperations.writePixels(px, w, h);
    }

    private boolean isStrongRed(int r, int g, int b, float hue, float sat, float val) {
        boolean hueIsRed = (hue >= 350f || hue <= 8f);

        if (!hueIsRed || sat < 0.55f || val < 0.12f) return false;

        int maxGB = Math.max(g, b);
        int diff = r - maxGB;

        // Absolute separation
        if (diff < 60) return false;

        // Relative separation vs green
        if (r < g * 1.35f) return false;

        // Relative separation vs blue
        if (r < b * 1.20f) return false;

        return true;
    }
}
