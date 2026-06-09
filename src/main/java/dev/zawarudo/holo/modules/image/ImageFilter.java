package dev.zawarudo.holo.modules.image;

import java.awt.image.BufferedImage;

public interface ImageFilter {

    String name();

    String description();

    BufferedImage apply(BufferedImage src, String[] args) throws IllegalArgumentException;
}
