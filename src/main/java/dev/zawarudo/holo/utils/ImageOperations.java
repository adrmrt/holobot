package dev.zawarudo.holo.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;

/**
 * A utility class for performing various image operations such as cropping,
 * joining, resizing, and more.
 */
public final class ImageOperations {

    private static final double RED_TO_GRAY_WEIGHT = 0.2126;
    private static final double GREEN_TO_GRAY_WEIGHT = 0.7152;
    private static final double BLUE_TO_GRAY_WEIGHT = 0.0722;

    private ImageOperations() {
        throw new UnsupportedOperationException();
    }

    /**
     * Crops an image to a circular shape.
     *
     * @param img The source BufferedImage.
     * @return A BufferedImage cropped to a circular shape.
     */
    public static BufferedImage cropToCircle(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = output.createGraphics();

        // Improve rendering quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.fill(new Ellipse2D.Double(0, 0, width, height));
        g2d.setComposite(AlphaComposite.SrcAtop);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        return output;
    }

    /**
     * Joins given images into a single image in the specified direction.
     *
     * @param direction The direction to join the images (HORIZONTAL or VERTICAL).
     * @param images    An array of BufferedImage objects to join.
     * @return A new BufferedImage containing all the provided images joined in the specified direction.
     */
    public static BufferedImage join(Direction direction, BufferedImage... images) {
        if (images == null || images.length == 0) {
            throw new IllegalArgumentException("Images array must not be empty");
        }

        int totalWidth = direction == Direction.HORIZONTAL ?
            Arrays.stream(images).mapToInt(BufferedImage::getWidth).sum() :
            Arrays.stream(images).mapToInt(BufferedImage::getWidth).max().orElse(0);
        int totalHeight = direction == Direction.VERTICAL ?
            Arrays.stream(images).mapToInt(BufferedImage::getHeight).sum() :
            Arrays.stream(images).mapToInt(BufferedImage::getHeight).max().orElse(0);

        BufferedImage combined = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = combined.createGraphics();

        // Draw each image in sequence, adjusting offset for each
        int offset = 0;
        for (BufferedImage img : images) {
            if (direction == Direction.HORIZONTAL) {
                g2d.drawImage(img, offset, 0, null);
                offset += img.getWidth();
            } else {
                g2d.drawImage(img, 0, offset, null);
                offset += img.getHeight();
            }
        }
        g2d.dispose();
        return combined;
    }

    /**
     * Resizes a BufferedImage to the specified width and height.
     *
     * @param img       = The BufferedImage to resize.
     * @param newWidth  = The desired width.
     * @param newHeight = The desired height.
     * @return The resized BufferedImage.
     */
    public static BufferedImage resize(BufferedImage img, int newWidth, int newHeight) {
        Image temp = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resizedImg.createGraphics();
        g2d.drawImage(temp, 0, 0, null);
        g2d.dispose();

        return resizedImg;
    }

    /**
     * Converts an image into a square by either cropping or scaling it.
     *
     * @param image           The image to transform.
     * @param length          The desired side length of the square.
     * @param backgroundColor Background color for scaling without cropping.
     * @param cropToFit       True to crop the image, false to scale.
     * @return A squared version of the original image.
     */
    public static BufferedImage squarefy(BufferedImage image, int length, Color backgroundColor, boolean cropToFit) {
        BufferedImage result = new BufferedImage(length, length, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setPaint(backgroundColor);
        graphics.fillRect(0, 0, length, length);

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int newX = 0, newY = 0, newWidth = imageWidth, newHeight = imageHeight;

        if (cropToFit) {
            if (imageWidth > imageHeight) {
                newX = (imageWidth - imageHeight) / 2;
                newWidth = imageHeight;
            } else {
                newY = (imageHeight - imageWidth) / 2;
                newHeight = imageWidth;
            }
        } else {
            float widthRatio = length / (float) imageWidth;
            float heightRatio = length / (float) imageHeight;
            float ratio = Math.min(widthRatio, heightRatio);
            newWidth = (int) (imageWidth * ratio);
            newHeight = (int) (imageHeight * ratio);
            newX = (length - newWidth) / 2;
            newY = (length - newHeight) / 2;
        }

        graphics.drawImage(image, newX, newY, newWidth, newHeight, null);
        graphics.dispose();
        return result;
    }

    /**
     * Converts all non-transparent pixels of an image to black.
     *
     * @param img The image to modify.
     * @return The image with all non-transparent pixels turned black.
     */
    public static BufferedImage turnBlack(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = img.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xff;
                if (alpha != 0) {
                    img.setRGB(x, y, (alpha << 24));
                }
            }
        }
        return img;
    }

    /**
     * Converts a {@link BufferedImage} into an {@link InputStream}
     *
     * @param img = A {@link BufferedImage}
     * @return An {@link InputStream}
     */
    public static InputStream toInputStream(BufferedImage img) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    /**
     * Converts an image to grayscale, maintaining original transparency.
     *
     * @param image The image to convert.
     * @return A grayscale version of the image.
     */
    public static BufferedImage convertToGrayScale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage grayscaleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                int grayscaleValue = calculateGrayscaleValue(red, green, blue);
                int grayscaleARGB = (alpha << 24) | (grayscaleValue << 16) | (grayscaleValue << 8) | grayscaleValue;
                grayscaleImage.setRGB(x, y, grayscaleARGB);
            }
        }
        return grayscaleImage;
    }

    public static int calculateGrayscaleValue(int red, int green, int blue) {
        return (int) (RED_TO_GRAY_WEIGHT * red + GREEN_TO_GRAY_WEIGHT * green + BLUE_TO_GRAY_WEIGHT * blue);
    }

    public static int[] readPixels(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        return img.getRGB(0, 0, w, h, null, 0, w);
    }

    public static BufferedImage writePixels(int[] pixels, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, width, height, pixels, 0, width);
        return out;
    }

    /**
     * Converts an RGB color to HSV.
     *
     * @param r Red channel [0–255]
     * @param g Green channel [0–255]
     * @param b Blue channel [0–255]
     * @return float[] { hue [0–360), saturation [0–1], value [0–1] }
     */
    public static float[] rgbToHsv(int r, int g, int b) {
        // Normalize RGB to [0..1] for stable floating-point math
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        // Find extrema and delta
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        // Value is highest intensity of the three colors
        float v = max;

        // Saturation is the "purity" of the color
        float s = max == 0f ? 0f : (delta / max);

        // Hue is calculated based on which color channel is the dominant one
        float h;
        if (delta == 0f) h = 0f;
        else if (max == rf) h = 60f * (((gf - bf) / delta) % 6f);
        else if (max == gf) h = 60f * (((bf - rf) / delta) + 2f);
        else h = 60f * (((rf - gf) / delta) + 4f);

        // Ensure hue is within [0..360)
        if (h < 0f) h += 360f;

        return new float[]{h, s, v};
    }

    /**
     * Converts an HSV color to RGB.
     *
     * @param h Hue in degrees [0–360)
     * @param s Saturation [0–1]
     * @param v Value (brightness) [0–1]
     * @return int RGB packed as 0xRRGGBB
     */
    public static int hsvToRgb(float h, float s, float v) {
        // Chroma defines the intensity of the color
        float c = v * s;

        // Map hue to one of six color sectors
        float hPrime = h / 60f;

        // Secondary component for interpolation within the sector
        float x = c * (1f - Math.abs(hPrime % 2f - 1f));

        // Offset to match the desired brightness
        float m = v - c;

        float rf, gf, bf;

        // Assign RGB based on the active hue sector
        if (hPrime < 1f) {
            rf = c;
            gf = x;
            bf = 0f;
        } else if (hPrime < 2f) {
            rf = x;
            gf = c;
            bf = 0f;
        } else if (hPrime < 3f) {
            rf = 0f;
            gf = c;
            bf = x;
        } else if (hPrime < 4f) {
            rf = 0f;
            gf = x;
            bf = c;
        } else if (hPrime < 5f) {
            rf = x;
            gf = 0f;
            bf = c;
        } else {
            rf = c;
            gf = 0f;
            bf = x;
        }

        // Convert back to [0..255] and clamp to avoid overflow
        int r = clamp255(Math.round((rf + m) * 255f));
        int g = clamp255(Math.round((gf + m) * 255f));
        int b = clamp255(Math.round((bf + m) * 255f));

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Clamps an integer value to the valid 8-bit color channel range.
     *
     * @param v Input value
     * @return Value clamped to [0–255]
     */
    public static int clamp255(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    /**
     * Clamps a floating-point value to the normalized range [0–1].
     *
     * @param v Input value
     * @return Value clamped to [0–1]
     */
    public static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    /**
     * The direction in which {@link BufferedImage}s should be joined.
     */
    public enum Direction {
        VERTICAL,
        HORIZONTAL
    }
}
