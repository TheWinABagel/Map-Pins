package dev.bagel.pins;

import dev.bagel.MapPinsPlugin;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;
@Value
@EqualsAndHashCode(exclude = { "color", "label" })
public class Pin {
    //ImageUtil.resizeImage(image, x, y)
    private static final BufferedImage SMALL_IMAGE = ImageUtil.loadImageResource(Pin.class, "/map_pin_small.png");
    private static final BufferedImage MEDIUM_IMAGE = ImageUtil.loadImageResource(Pin.class, "/map_pin_medium.png");
    private static final BufferedImage LARGE_IMAGE = ImageUtil.loadImageResource(Pin.class, "/map_pin_large.png");

    WorldPoint point;
    Style style;
    @Nullable
    Color color;
    @Nullable
    String label;

    @Getter
    public enum Style {
        SMALL(Pin.SMALL_IMAGE, 19, 29, true),
        MEDIUM(Pin.MEDIUM_IMAGE, 23, 36, true),
        LARGE(Pin.LARGE_IMAGE, 28, 43, true);

        private final BufferedImage image;
        private final int x;
        private final int y;
        private final boolean canBeColored;
        Style(BufferedImage image, int x, int y, boolean canBeColored) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.canBeColored = canBeColored;
        }
    }
}
