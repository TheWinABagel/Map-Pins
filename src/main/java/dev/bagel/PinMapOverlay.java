package dev.bagel;

import com.google.inject.Inject;
import dev.bagel.pins.WorldPin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;

import java.awt.*;
import java.awt.geom.Rectangle2D;

@Slf4j
public class PinMapOverlay extends Overlay {
    private static final Color WHITE_TRANSLUCENT = new Color(255, 255, 255, 127);
    private static final int LABEL_PADDING = 4;
    private static final int REGION_SIZE = 1 << 6;
    // Bitmask to return first coordinate in region
    private static final int REGION_TRUNCATE = ~0x3F;

    private final Client client;
    private final MapPinsPlugin plugin;
    private final MapPinsConfig config;
    private final WorldMapOverlay worldMapOverlay;

    @Inject
    private PinMapOverlay(Client client, MapPinsPlugin plugin, MapPinsConfig config, WorldMapOverlay worldMapOverlay) {
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(0.9f);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.worldMapOverlay = worldMapOverlay;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.getPoints().isEmpty()) {
            drawRegionOverlay(graphics);
        }

        return null;
    }

    private WorldPoint calculateMapPoint(Point point) {
        //Zoom for map
        float zoom = client.getWorldMap().getWorldMapZoom();
        //current bottom left pos of map
        WorldPoint mapPoint = new WorldPoint(client.getWorldMap().getWorldMapPosition().getX(), client.getWorldMap().getWorldMapPosition().getY(), 0);
        //Screen coords for the middle map point
        Point middle = worldMapOverlay.mapWorldPointToGraphicsPoint(mapPoint);

        //the X offset from the middle
        int dx = (int) ((point.getX() - middle.getX()) / zoom);
        log.info("dx: {}, (point.getX() {} - middle.getX() {}) / zoom {}", dx, point.getX(), middle.getX(), zoom);
        //the Y offset from the middle
        int dy = (int) ((-(point.getY() - middle.getY())) / zoom);
        log.info("dy: {}, ((-(point.getY(): {} - middle.getY(): {})) / zoom {})", dy, point.getX(), middle.getX(), zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    private void drawRegionOverlay(Graphics2D graphics) {
        Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);

        if (map == null) return;

        WorldMap worldMap = client.getWorldMap();
        float pixelsPerTile = worldMap.getWorldMapZoom();
        Rectangle worldMapRect = map.getBounds();
        graphics.setClip(worldMapRect);

        int mapWidthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
        int mapHeightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

        net.runelite.api.Point southWestWorldMapPosition = worldMap.getWorldMapPosition();

        // Offset in tiles from anchor sides
        int yTileMin = southWestWorldMapPosition.getY() - mapHeightInTiles / 2;
        int xRegionMin = (southWestWorldMapPosition.getX() - mapWidthInTiles / 2)/* & REGION_TRUNCATE*/;
        int xRegionMax = ((southWestWorldMapPosition.getX() + mapWidthInTiles / 2)/* & REGION_TRUNCATE*/) + REGION_SIZE;
        int yRegionMin = (yTileMin/* & REGION_TRUNCATE*/);
        int yRegionMax = ((southWestWorldMapPosition.getY() + mapHeightInTiles / 2)/* & REGION_TRUNCATE*/) + REGION_SIZE;
        int regionPixelSize = (int) Math.ceil(REGION_SIZE * pixelsPerTile);

        Point mousePos = client.getMouseCanvasPosition();

//        plugin.setHoveredRegion(-1);
        graphics.setColor(WHITE_TRANSLUCENT);
        for (int i = 0; i < plugin.getPoints().size(); i++) {
            WorldPin pin = plugin.getPoints().get(i);
            WorldPoint southWestPinCorner = pin.getArea().toWorldPoint();
            int yTileOffset = -(southWestWorldMapPosition.getY() - southWestPinCorner.getY());
            int xTileOffset = southWestPinCorner.getX() /*+ mapWidthInTiles / 2*/ - southWestWorldMapPosition.getX();

            int xPos = ((int) (xTileOffset)) /* pixelsPerTile)) + (int) worldMapRect.getX()*/;
            int yPos = (worldMapRect.height - (int) (yTileOffset)) /* pixelsPerTile)) + (int) worldMapRect.getY()*/;


            Rectangle regionRect = new Rectangle(southWestPinCorner.getX(), southWestPinCorner.getY() /*xPos, yPos*/, pin.getArea().getWidth(), pin.getArea().getHeight());
//            log.info("Region rect: {}", regionRect);
            log.info("Current mouse pos: {}, region rect: {}", mousePos, regionRect);
            if (/*containsRegion || unlockable || blacklisted || */true) {
                Color color = config.pinColor();
                if (regionRect.contains(mousePos.getX(), mousePos.getY())) {
                    log.info("Hovering pin!");
                    color = color.brighter();
                }
                graphics.setColor(color);
                graphics.fillRect(xPos, yPos, pin.getArea().getWidth(), pin.getArea().getHeight());
            }
        }
        if (false)
            for (int x = xRegionMin; x < xRegionMax; x += REGION_SIZE) {
            for (int y = yRegionMin; y < yRegionMax; y += REGION_SIZE) {
                int yTileOffset = -(yTileMin - y);
                int xTileOffset = x + mapWidthInTiles / 2 - southWestWorldMapPosition.getX();

                int xPos = ((int) (xTileOffset * pixelsPerTile)) + (int) worldMapRect.getX();
                int yPos = (worldMapRect.height - (int) (yTileOffset * pixelsPerTile)) + (int) worldMapRect.getY();
                // Offset y-position by a single region to correct for drawRect starting from the top
                yPos -= regionPixelSize;

                int regionId = ((x >> 6) << 8) | (y >> 6);
                String regionText = String.valueOf(regionId);
                FontMetrics fm = graphics.getFontMetrics();
                Rectangle2D textBounds = fm.getStringBounds(regionText, graphics);
                Rectangle regionRect = new Rectangle(xPos, yPos, regionPixelSize, regionPixelSize);

//                RegionTypes regionType = RegionLocker.getType(regionId);
//                boolean containsRegion = (regionType != null) ^ config.invertMapOverlay();
//                boolean unlockable = regionType == RegionTypes.UNLOCKABLE;
//                boolean blacklisted = regionType == RegionTypes.BLACKLISTED;
                if (/*containsRegion || unlockable || blacklisted || */true) {
                    Color color = config.pinColor();
                    if (regionRect.contains(mousePos.getX(), mousePos.getY()))
                        color = color.brighter();
                    graphics.setColor(color);
                    graphics.fillRect(xPos, yPos, regionPixelSize, regionPixelSize);
                }


//                if (regionRect.contains(mousePos.getX(), mousePos.getY()))
//                    plugin.setHoveredRegion(regionId);

                graphics.setColor(new Color(0, 19, 36, 127));
//                if (config.drawMapGrid())
                    graphics.drawRect(xPos, yPos, regionPixelSize, regionPixelSize);

                graphics.setColor(WHITE_TRANSLUCENT);
//                if (config.drawRegionId())
                    graphics.drawString(regionText, xPos + LABEL_PADDING, yPos + (int) textBounds.getHeight() + LABEL_PADDING);
            }
        }

        int currentId = client.getLocalPlayer().getWorldLocation().getRegionID();
        String regionText = String.valueOf(currentId);
        FontMetrics fm = graphics.getFontMetrics();
        Rectangle2D textBounds = fm.getStringBounds(regionText, graphics);
/*        if (config.drawRegionId()) {
            if (plugin.getHoveredRegion() >= 0)
                graphics.drawString("Hovered chunk: " + regionLockerPlugin.getHoveredRegion(), (int) worldMapRect.getX() + LABEL_PADDING, (int) (worldMapRect.getY() + worldMapRect.getHeight()) - LABEL_PADDING - (int) textBounds.getHeight());
            graphics.drawString("Player chunk: " + regionText, (int) worldMapRect.getX() + LABEL_PADDING, (int) (worldMapRect.getY() + worldMapRect.getHeight()) - LABEL_PADDING);
        }*/

    }
}
