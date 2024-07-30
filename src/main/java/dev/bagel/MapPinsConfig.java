package dev.bagel;

import dev.bagel.pins.Pin;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("mapPins")
public interface MapPinsConfig extends Config {

    @Alpha
    @ConfigItem(
            keyName = "defaultPinColor",
            name = "Default Pin Color",
            description = "Configures the default color of created pins."
    )
    default Color pinColor() {
        return new Color(255, 0, 0, 255);
    }

    @ConfigItem(
            keyName = "drawPinOnMinimap",
            name = "Draw pins on minimap",
            description = "Configures whether pins should be drawn on minimap."
    )
    default boolean drawPinOnMinimap() {
        return true;
    }

    @ConfigItem(
            keyName = "pinStyle",
            name = "Default pin style",
            description = "Configures the default size and style for pins."
    )
    default Pin.Style pinStyle() {
        return Pin.Style.SMALL;
    }

    @ConfigItem(
            keyName = "resetPins",
            name = "Reset pins",
            description = "Resets the pins on the map. WARNING: NON REVERSIBLE",
            warning = "Are you sure you want to remove *all* pins? This action is not reversible."
    )
    default boolean resetPins() {
        return false;
    }
}
