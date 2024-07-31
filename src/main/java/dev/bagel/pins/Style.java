package dev.bagel.pins;

import lombok.Getter;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@Getter
public enum Style {
    DEFAULT(ImageResources.DEFAULT_PIN, 19, 29, true);

    private final BufferedImage image;
    private final int defaultX;
    private final int defaultY;
    private final boolean canBeColored;
    Style(BufferedImage image, int defaultX, int defaultY, boolean canBeColored) {
        this.image = image;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.canBeColored = canBeColored;
    }

    private final static class ImageResources {
        public static final BufferedImage DEFAULT_PIN = ImageUtil.loadImageResource(dev.bagel.pins.Style.class, "/map_pin_small.png");
    }
}