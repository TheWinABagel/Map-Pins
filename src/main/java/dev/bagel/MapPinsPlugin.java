package dev.bagel;

import com.google.gson.Gson;
import com.google.inject.Provides;
import dev.bagel.pins.WorldPin;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

import javax.inject.Inject;
import java.util.List;
import java.util.*;

@Slf4j
@PluginDescriptor(
        name = "Map pins"
)
public class MapPinsPlugin extends Plugin {
    /* Map pins: can be named, and change color.
    Can also render in world, with that color.
    Multiple different pin styles
     * */
    private static final String CONFIG_GROUP = "mapPins";
    private static final String SAVED_PINS_LOCATION = "saved_pins";

    @Getter(AccessLevel.PACKAGE)
    private final List<WorldPin> points = new ArrayList<>();
    @Inject
    private Client client;
    @Inject
    private MapPinsConfig config;
    @Inject
    private ConfigManager configManager;
    @Inject
    WorldMapOverlay worldMapOverlay;
    @Inject
    private Gson gson;
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PinMinimapOverlay minimapOverlay;

    @Inject
    private PinMapOverlay mapOverlay;

    @Inject
    private ChatboxPanelManager chatboxPanelManager;

    @Inject
    private EventBus eventBus;
    @Inject
    private WorldMapPointManager worldMapPointManager;

    @Inject
    private ColorPickerManager colorPickerManager;

    @Provides
    MapPinsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MapPinsConfig.class);
    }

    @Override
    public void startUp() throws Exception {
        overlayManager.add(minimapOverlay);
        overlayManager.add(mapOverlay);
//        loadPoints();
        points.add(new WorldPin(new WorldArea(250, 250, 250, 250, 0), config.pinStyle(), config.pinColor(), ""));
    }

    @Override
    public void shutDown() throws Exception {
        overlayManager.remove(minimapOverlay);
        overlayManager.remove(mapOverlay);
        points.clear();
    }


    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
    }

/*    private void createPin(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return;
        }
        int regionId = worldPoint.getRegionID();

        PinPoint point = new PinPoint(regionId, worldPoint, config.pinStyle(), config.pinColor(), null);
        log.debug("Updating point: {} - {}", point, worldPoint);

        PinWorldMapPoint marker = new PinWorldMapPoint(point*//*, changeColor(config.pinStyle().getImage(), config.pinColor())*//*);
        marker.setName("Pin");

        List<PinPoint> groundMarkerPoints = new ArrayList<>(getPoints(regionId));

        log.info("groundMarkerPoints does not contain point {}, \nallPoints: {}", point, groundMarkerPoints);
        worldMapPointManager.add(marker);
        groundMarkerPoints.add(point);

        log.info("marker being added: {}", marker);


        savePoints(regionId, groundMarkerPoints);

        loadPoints();
    }*/
}
