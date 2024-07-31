package dev.bagel.pins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;

/** Deserialized pin. Not how they are saved normally
 * */
@Value
@EqualsAndHashCode(exclude = { "color", "label" })
public class WorldPin {
    //Where the pin was generated
    WorldArea area;
    Style style;
    Color color;
    String label;

    public SerializablePin toSerializable() {
        return new SerializablePin(area, style, color, label);
    }



}