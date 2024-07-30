package dev.bagel;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import dev.bagel.pins.Pin;
import dev.bagel.pins.PinPoint;
import dev.bagel.pins.PinWorldMapPoint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    private static final String WALK_HERE = "Walk here";
    private static final String REGION_PREFIX = "region_";
    @Getter(AccessLevel.PACKAGE)
    private final List<Pin> points = new ArrayList<>();
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
    private ChatboxPanelManager chatboxPanelManager;

    @Inject
    private EventBus eventBus;
    @Inject
    private WorldMapPointManager worldMapPointManager;

    @Inject
    private ColorPickerManager colorPickerManager;

    public static BufferedImage changeColor(BufferedImage image, Color replacement_color) {

        BufferedImage dimg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dimg.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(image, null, 0, 0);//w  w w  .ja  v a 2  s . co m
        g.dispose();
        for (int i = 0; i < dimg.getHeight(); i++) {
            for (int j = 0; j < dimg.getWidth(); j++) {
                int argb = dimg.getRGB(j, i);
                int alpha = (argb >> 24) & 0xff;
                if (alpha > 0) {
                    Color col = new Color(replacement_color.getRed(), replacement_color.getGreen(), replacement_color.getBlue(), alpha);
                    dimg.setRGB(j, i, col.getRGB());
                }
            }
        }
        return dimg;
    }

    public void savePoints(int regionId, Collection<PinPoint> points) {
        if (points == null || points.isEmpty()) {
            configManager.unsetConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);
            return;
        }

        String json = gson.toJson(points);
        configManager.setConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId, json);
    }

    public Collection<PinPoint> getPoints(int regionId) {
        String json = configManager.getConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);

        if (Strings.isNullOrEmpty(json)) {
            return Collections.emptyList();
        }

        return gson.fromJson(json, new TypeToken<List<PinPoint>>() {
        }.getType());
    }

    void loadPoints() {
        points.clear();

        int[] regions = client.getMapRegions();

        if (regions == null) {
            return;
        }

        for (int regionId : regions) {
            // load points for region
            log.debug("Loading points for region {}", regionId);
            Collection<PinPoint> regionPoints = getPoints(regionId);
            Collection<Pin> pins = translateToPin(regionPoints);
            points.addAll(pins);
        }
    }

/*    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals(MapPinsConfig.GROUND_MARKER_CONFIG_GROUP)
                && event.getKey().equals(MapPinsConfig.SHOW_IMPORT_EXPORT_KEY_NAME))
        {
            sharingManager.removeMenuOptions();

            if (config.showImportExport())
            {
                sharingManager.addImportExportMenuOptions();
                sharingManager.addClearMenuOption();
            }
        }
    }*/

    /**
     * Translate a collection of ground marker points to color tile markers, accounting for instances
     *
     * @param points {@link PinPoint}s to be converted to {@link Pin}s
     * @return A collection of color tile markers, converted from the passed ground marker points, accounting for local
     * instance points. See {@link WorldPoint#toLocalInstance(Client, WorldPoint)}
     */
    private Collection<Pin> translateToPin(Collection<PinPoint> points) {
        if (points.isEmpty()) {
            return Collections.emptyList();
        }

        return points.stream()
                .map(point -> new Pin(point.toWorldPoint(), config.pinStyle(), point.getColor(), point.getLabel()))
                .flatMap(pin -> {
                    Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(client.getTopLevelWorldView(), pin.getPoint());
                    return localWorldPoints.stream().map(wp -> new Pin(wp, pin.getStyle(), pin.getColor(), pin.getLabel()));
                })
                .collect(Collectors.toList());
    }

    @Override
    public void startUp() {
        overlayManager.add(minimapOverlay);
        loadPoints();
    }

    @Override
    public void shutDown() {
        overlayManager.remove(minimapOverlay);
        points.clear();
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged profileChanged) {
        loadPoints();

    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            loadPoints();
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);

        if (map == null) {
            return;
        } else if (!map.getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY())) {
            return;
        }


        WorldPoint clickedWorldPoint = calculateMapPoint(client.getMouseCanvasPosition());

        final int regionId = clickedWorldPoint.getRegionID();
        Collection<PinPoint> regionPoints = getPoints(regionId);
        Optional<PinPoint> existingOpt = regionPoints.stream()
                .filter(p -> pointContainsCursor(clickedWorldPoint, p.getStyle().getImage()))
                .findFirst();


        client.createMenuEntry(0)
                .setOption("Create Pin")
                .onClick(menuEntry -> {
//                    if (existingOpt.isPresent()) {
//                        markTile(existingOpt.get().toWorldPoint());
//                        log.info("removing a pin at x: {}, y: {}, with region id {}!", worldPoint.getRegionX(), worldPoint.getRegionY(), worldPoint.getRegionID());
//                    } else {

                    createPin(clickedWorldPoint);
//                    log.info("added a new pin at x: {}, y: {}, with region id {}!", worldPoint.getX(), worldPoint.getY(), worldPoint.getRegionID());
//                    }
                });

        for (MenuEntry entry : event.getMenuEntries()) {
            if (entry.getTarget().contains("Pin")) {


                client.createMenuEntry(0)
                        .setOption("Remove pin: ")
                        .setTarget(entry.getTarget())
                        .onClick(e -> {
                            if (existingOpt.isPresent()) {
                                log.warn("Calling remove pin on {}", existingOpt.get());
                                removePin(existingOpt.get().toWorldPoint(), clickedWorldPoint);
                            }
                            else {
                                log.error("Cannot remove pin as it does not exist in the area");
                            }
//                                    markTile(worldPoint);
   /*                                 if (markers.containsKey(pinOwner)) {
                                        worldMapPointManager.removeIf(x -> x == markers.get(pinOwner));
                                        markers.put(pinOwner, null);
                                    }*/
                                }
                        );
                client.createMenuEntry(1)
                        .setOption("Rename Pin: ")
                        .setTarget(entry.getTarget())
                        .onClick(e -> {
                            if (existingOpt.isPresent()) {
                                labelPin(existingOpt.get(), clickedWorldPoint);
                            }
                            else {
                                log.error("Cannot label pin as it does not exist in the area!");
                            }

                        });
            }
        }
    }

    private boolean pointContainsCursor(WorldPoint point, BufferedImage image/*, Point imagePoint*/) {
        Widget widget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (widget == null) {
            return false;
        }
        Point mousePos = client.getMouseCanvasPosition();
        if (!getWorldMapClipArea(widget.getBounds()).contains(mousePos.getX(), mousePos.getY())) {
            mousePos = null;
        }
        Point drawPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(point);
        int drawX = drawPoint.getX();
        int drawY = drawPoint.getY();

//        if (imagePoint == null) {
            drawX -= image.getWidth() / 2;
            drawY -= image.getHeight() / 2;
//        } else {
//            drawX -= imagePoint.getX();
//            drawY -= imagePoint.getY();
//        }

        Rectangle clickbox = new Rectangle(drawX, drawY, image.getWidth(), image.getHeight());
        if (mousePos != null && clickbox.contains(mousePos.getX(), mousePos.getY())) {
            return true;
        }
        return false;
    }

    private WorldPoint calculateMapPoint(Point point) {
        float zoom = client.getWorldMap().getWorldMapZoom();
        WorldPoint mapPoint = new WorldPoint(client.getWorldMap().getWorldMapPosition().getX(), client.getWorldMap().getWorldMapPosition().getY(), 0);
        Point middle = worldMapOverlay.mapWorldPointToGraphicsPoint(mapPoint);

        int dx = (int) ((point.getX() - middle.getX()) / zoom);
        int dy = (int) ((-(point.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    /**
     * Gets a clip area which excludes the area of widgets which overlay the world map.
     *
     * @param baseRectangle The base area to clip from
     * @return An {@link Area} representing <code>baseRectangle</code>, with the area
     * of visible widgets overlaying the world map clipped from it.
     */
    private Shape getWorldMapClipArea(Rectangle baseRectangle) {
        final Widget overview = client.getWidget(ComponentID.WORLD_MAP_OVERVIEW_MAP);
        final Widget surfaceSelector = client.getWidget(ComponentID.WORLD_MAP_SURFACE_SELECTOR);

        Area clipArea = new Area(baseRectangle);
        boolean subtracted = false;

        if (overview != null && !overview.isHidden()) {
            clipArea.subtract(new Area(overview.getBounds()));
            subtracted = true;
        }

        if (surfaceSelector != null && !surfaceSelector.isHidden()) {
            clipArea.subtract(new Area(surfaceSelector.getBounds()));
            subtracted = true;
        }

        // The sun g2d implementation is much more efficient at applying clips which are subclasses of rectangle2d,
        // so use that as the clip shape if possible
        return subtracted ? clipArea : baseRectangle;
    }

    @Provides
    MapPinsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MapPinsConfig.class);
    }

    private void createPin(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return;
        }
        int regionId = worldPoint.getRegionID();

        PinPoint point = new PinPoint(regionId, worldPoint, config.pinStyle(), config.pinColor(), null);
        log.debug("Updating point: {} - {}", point, worldPoint);

        PinWorldMapPoint marker = new PinWorldMapPoint(point/*, changeColor(config.pinStyle().getImage(), config.pinColor())*/);
        marker.setName("Pin");

        List<PinPoint> groundMarkerPoints = new ArrayList<>(getPoints(regionId));

        log.info("groundMarkerPoints does not contain point {}, \nallPoints: {}", point, groundMarkerPoints);
        worldMapPointManager.add(marker);
        groundMarkerPoints.add(point);

        log.info("marker being added: {}", marker);


        savePoints(regionId, groundMarkerPoints);

        loadPoints();
    }

    private void removePin(WorldPoint removedWorldPoint, WorldPoint clickedWorldPoint) {
        if (removedWorldPoint == null || clickedWorldPoint == null) {
            return;
        }
        int regionId = removedWorldPoint.getRegionID();

        PinPoint pinPoint = new PinPoint(regionId, removedWorldPoint, config.pinStyle(), config.pinColor(), null);
        log.debug("Updating point: {} - {}", pinPoint, removedWorldPoint);

        PinWorldMapPoint marker = new PinWorldMapPoint(pinPoint);
        marker.setName("Pin");

        List<PinPoint> groundMarkerPoints = new ArrayList<>(getPoints(regionId));

//        AtomicBoolean firstFound = new AtomicBoolean(false);
        worldMapPointManager.removeIf(worldMapPoint -> {
//            if (firstFound.get()) return false;
            log.warn("world map zoom currently is {}", client.getWorldMap().getWorldMapZoom());
            boolean test = worldMapPoint instanceof PinWorldMapPoint && clickedWorldPoint.distanceTo2D(worldMapPoint.getWorldPoint()) <(int) ((pinPoint.getStyle().getY())/ client.getWorldMap().getWorldMapZoom() );/*pinPoint.doesWorldPointMatch(worldMapPoint.getWorldPoint());*/
            if (test) {
                log.warn("sucessfully removing marker: {}, world map point {}", marker, worldMapPoint.getWorldPoint());
//                firstFound.set(true);
            }
            return test;
        });
        groundMarkerPoints.remove(pinPoint);

        savePoints(regionId, groundMarkerPoints);

        loadPoints();
    }

    private void markTile(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return;
        }

//        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

        int regionId = worldPoint.getRegionID();

        PinPoint point = new PinPoint(regionId, worldPoint, config.pinStyle(), config.pinColor(), null);
        log.debug("Updating point: {} - {}", point, worldPoint);

        WorldMapPoint marker = new WorldMapPoint(worldPoint, changeColor(config.pinStyle().getImage(), config.pinColor()));

        final String tooltip = "Pin tooltip here";
//        marker.setImagePoint(new Point(config.pinStyle().getX(), config.pinStyle().getY()));

        marker.setTooltip(tooltip);
        marker.setName("Pin #" + points.size());
        marker.setTarget(marker.getWorldPoint());
        marker.setJumpOnClick(true);
        marker.setSnapToEdge(true);


        List<PinPoint> groundMarkerPoints = new ArrayList<>(getPoints(regionId));
        if (groundMarkerPoints.contains(point)) {
            log.info("groundMarkerPoints contains point {} ", point);
            worldMapPointManager.removeIf(worldMapPoint -> {
                return tooltip.equals(worldMapPoint.getTooltip()) && worldMapPoint.getWorldPoint().distanceTo2D(marker.getWorldPoint()) == 0;
            });
            groundMarkerPoints.remove(point);
            log.info("marker being removed: {} ", marker);
        } else {
            log.info("groundMarkerPoints does not contain point {}, \nallPoints: {}", point, groundMarkerPoints);
            worldMapPointManager.add(marker);
            groundMarkerPoints.add(point);

            log.info("marker being added: {}", marker);
        }

        savePoints(regionId, groundMarkerPoints);

        loadPoints();
    }

    private void labelPin(PinPoint existing, WorldPoint clickedWorldPoint) {
        if (clickedWorldPoint == null) {
            return;
        }
        chatboxPanelManager.openTextInput("Pin label")
                .value(Optional.ofNullable(existing.getLabel()).orElse(""))
                .onDone((input) ->
                {
                    input = Strings.emptyToNull(input);

                    PinPoint newPoint = new PinPoint(existing.getRegionId(), existing.toWorldPoint(), existing.getStyle(), existing.getColor(), input);
                    Collection<PinPoint> points = new ArrayList<>(getPoints(existing.getRegionId()));
                    worldMapPointManager.removeIf(worldMapPoint -> {
                        return worldMapPoint instanceof PinWorldMapPoint && clickedWorldPoint.distanceTo2D(worldMapPoint.getWorldPoint()) < (int) ((existing.getStyle().getY())/ client.getWorldMap().getWorldMapZoom() );
                    });
                    var test = new PinWorldMapPoint(newPoint);
                    test.setName(input);
                    worldMapPointManager.add(test);
                    points.remove(existing);
                    points.add(newPoint);
                    savePoints(existing.getRegionId(), points);

                    loadPoints();
                })
                .build();
    }

    private void colorPin(PinPoint existing, Color newColor) {
        var newPoint = new PinPoint(existing.getRegionId(), existing, existing.getStyle(), newColor, existing.getLabel());
        Collection<PinPoint> points = new ArrayList<>(getPoints(existing.getRegionId()));
        points.remove(newPoint);
        points.add(newPoint);
        savePoints(existing.getRegionId(), points);

        loadPoints();
    }

    private void changePinStyle(PinPoint existing, Pin.Style newStyle) {
        var newPoint = new PinPoint(existing.getRegionId(), existing, newStyle, existing.getColor(), existing.getLabel());
        Collection<PinPoint> points = new ArrayList<>(getPoints(existing.getRegionId()));
        points.remove(newPoint);
        points.add(newPoint);
        savePoints(existing.getRegionId(), points);

        loadPoints();
    }
}
