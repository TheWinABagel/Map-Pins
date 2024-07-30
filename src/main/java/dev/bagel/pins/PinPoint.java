package dev.bagel.pins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.awt.*;

@Value
@Slf4j
@EqualsAndHashCode(exclude = { "color", "label" })
public class PinPoint {
    int regionId;
    int regionX;
    int regionY;
    int x;
    int y;
    int z;
    Pin.Style style;
    @Nullable
    Color color;
    @Nullable
    String label;

    public PinPoint(int regionId, PinPoint point, Pin.Style style, @Nullable Color color, @Nullable String label) {
        this(regionId, point.toWorldPoint(), style, color, label);
    }

    public PinPoint(int regionId, WorldPoint point, Pin.Style style, @Nullable Color color, @Nullable String label) {
        this.regionId = regionId;
        this.regionX = point.getRegionX();
        this.regionY = point.getRegionY();
        this.x = point.getX();
        this.y = point.getY();
        this.z = point.getPlane();
        this.style = style;
        this.color = color;
        this.label = label;
    }

    public boolean doesWorldPointMatch(WorldPoint worldPoint) {
        WorldArea thisPos = new WorldArea(x, y, style.getX(), style.getY(), z);
        WorldArea otherPos = new WorldArea(worldPoint.getX(), worldPoint.getY(), 5, 5, worldPoint.getPlane());
        return thisPos.intersectsWith(otherPos);
    }

    public WorldPoint toWorldPoint() {
        return new WorldPoint(x, y, z);
    }
}
