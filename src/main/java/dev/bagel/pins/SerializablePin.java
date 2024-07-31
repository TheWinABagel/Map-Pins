package dev.bagel.pins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;

@Value
@Slf4j
@EqualsAndHashCode(exclude = {"color", "label"})
public class SerializablePin {
//    int centerpointX;
//    int centerpointY;
    int plane;
    int southWestX;
    int southWestY;

    int areaWidth;
    int areaHeight;
    Style style;
    Color color;
    String label;

    public SerializablePin(WorldArea area, Style style, Color color, String label) {

        //Area info
        this.plane = area.getPlane();
        this.southWestX = area.getX();
        this.southWestY = area.getY();
        this.areaWidth = area.getWidth();
        this.areaHeight = area.getHeight();

        this.style = style;
        this.color = color;
        this.label = label;
    }


}
