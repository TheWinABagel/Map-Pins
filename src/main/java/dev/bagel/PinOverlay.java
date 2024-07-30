package dev.bagel;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import dev.bagel.pins.PinWorldMapPoint;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ReflectUtil;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;


@Slf4j
public class PinOverlay {
    private final Client client;
    private final MapPinsPlugin plugin;
    private WorldMapPoint hoveredPoint;
    private final WorldMapPointManager worldMapPointManager;


    public PinOverlay(Client client, MapPinsPlugin plugin, WorldMapPointManager worldMapPointManager) {
        this.client = client;
        this.plugin = plugin;
        this.worldMapPointManager = worldMapPointManager;
    }

    private WorldPoint calculateMapPoint(Point point) {
        float zoom = client.getWorldMap().getWorldMapZoom();
        WorldPoint mapPoint = new WorldPoint(client.getWorldMap().getWorldMapPosition().getX(), client.getWorldMap().getWorldMapPosition().getY(), 0);
        Point middle = plugin.worldMapOverlay.mapWorldPointToGraphicsPoint(mapPoint);

        int dx = (int) ((point.getX() - middle.getX()) / zoom);
        int dy = (int) ((-(point.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    private boolean calculateBagel(WorldMapPoint worldPoint) {
        Widget widget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (widget == null) {
            return false;
        }
        Point mousePos = client.getMouseCanvasPosition();
        if (!getWorldMapClipArea(widget.getBounds()).contains(mousePos.getX(), mousePos.getY())) {
            mousePos = null;
        }
        WorldPoint point = worldPoint.getWorldPoint();
        Point drawPoint = mapWorldPointToGraphicsPoint(point);
        BufferedImage image = worldPoint.getImage();
        int drawX = drawPoint.getX();
        int drawY = drawPoint.getY();

        if (worldPoint.getImagePoint() == null) {
            drawX -= image.getWidth() / 2;
            drawY -= image.getHeight() / 2;
        } else {
            drawX -= worldPoint.getImagePoint().getX();
            drawY -= worldPoint.getImagePoint().getY();
        }

        Rectangle clickbox = new Rectangle(drawX, drawY, image.getWidth(), image.getHeight());
        if (mousePos != null && clickbox.contains(mousePos.getX(), mousePos.getY())) {
            return true;
        }
        return false;
    }

    public Dimension render(/*Graphics2D graphics*/) {
        java.util.List<WorldMapPoint> points;
        try {
            Field worldMapPoints = WorldMapPointManager.class.getDeclaredField("worldMapPoints");
            worldMapPoints.setAccessible(true);
            points = (java.util.List<WorldMapPoint>) worldMapPoints.get(worldMapPointManager);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Failed to get world map, Map pins won't work properly!");
            points = new ArrayList<>();
        }




        if (points.isEmpty()) {
            return null;
        }

        Widget widget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        Widget bottomBar = client.getWidget(ComponentID.WORLD_MAP_BOTTOM_BAR);
        if (widget == null || bottomBar == null) {
            return null;
        }

        bottomBar.setOnTimerListener((JavaScriptCallback) ev -> {
            WorldMapPoint worldPoint = hoveredPoint;
            if (client.isMenuOpen() || worldPoint == null) {
                return;
            }

            client.createMenuEntry(-1)
                    .setTarget(ColorUtil.wrapWithColorTag(worldPoint.getName(), JagexColors.MENU_TARGET))
                    .setOption("Focus on")
                    .setType(MenuAction.RUNELITE)
                    .onClick(m -> client.getWorldMap().setWorldMapPositionTarget(
                            MoreObjects.firstNonNull(worldPoint.getTarget(), worldPoint.getWorldPoint())));
        });
        bottomBar.setHasListener(true);

        final Rectangle worldMapRectangle = widget.getBounds();
        final Shape mapViewArea = getWorldMapClipArea(worldMapRectangle);
        final Rectangle canvasBounds = new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
        final Shape canvasViewArea = getWorldMapClipArea(canvasBounds);
        Shape currentClip = null;

        Point mousePos = client.getMouseCanvasPosition();
        if (!mapViewArea.contains(mousePos.getX(), mousePos.getY())) {
            mousePos = null;
        }

        hoveredPoint = null;

        WorldMapPoint tooltipPoint = null;

        for (WorldMapPoint worldPoint : points) {
            BufferedImage image = worldPoint.getImage();
            WorldPoint point = worldPoint.getWorldPoint();

            if (image != null && point != null) {
                Point drawPoint = mapWorldPointToGraphicsPoint(point);
                if (drawPoint == null) {
                    continue;
                }

/*                if (worldPoint.isSnapToEdge() && canvasViewArea != currentClip) {
                    graphics.setClip(canvasViewArea);
                    currentClip = canvasViewArea;
                } else if (!worldPoint.isSnapToEdge() && mapViewArea != currentClip) {
                    graphics.setClip(mapViewArea);
                    currentClip = mapViewArea;
                }*/

                if (worldPoint.isSnapToEdge()) {
                    // Get a smaller rect for edge-snapped icons so they display correctly at the edge
                    final Rectangle snappedRect = widget.getBounds();
                    snappedRect.grow(-image.getWidth() / 2, -image.getHeight() / 2);

                    final Rectangle unsnappedRect = new Rectangle(snappedRect);
                    if (worldPoint.getImagePoint() != null) {
                        int dx = worldPoint.getImagePoint().getX() - (image.getWidth() / 2);
                        int dy = worldPoint.getImagePoint().getY() - (image.getHeight() / 2);
                        unsnappedRect.translate(dx, dy);
                    }
                    // Make the unsnap rect slightly smaller so a smaller snapped image doesn't cause a freak out
                    if (worldPoint.isCurrentlyEdgeSnapped()) {
                        unsnappedRect.grow(-image.getWidth(), -image.getHeight());
                    }

                    if (unsnappedRect.contains(drawPoint.getX(), drawPoint.getY())) {
                        if (worldPoint.isCurrentlyEdgeSnapped()) {
                            worldPoint.setCurrentlyEdgeSnapped(false);
                            worldPoint.onEdgeUnsnap();
                        }
                    } else {
                        drawPoint = clipToRectangle(drawPoint, snappedRect);
                        if (!worldPoint.isCurrentlyEdgeSnapped()) {
                            worldPoint.setCurrentlyEdgeSnapped(true);
                            worldPoint.onEdgeSnap();
                        }
                    }
                }

                int drawX = drawPoint.getX();
                int drawY = drawPoint.getY();

                if (worldPoint.getImagePoint() == null) {
                    drawX -= image.getWidth() / 2;
                    drawY -= image.getHeight() / 2;
                } else {
                    drawX -= worldPoint.getImagePoint().getX();
                    drawY -= worldPoint.getImagePoint().getY();
                }

//                graphics.drawImage(image, drawX, drawY, null);
                Rectangle clickbox = new Rectangle(drawX, drawY, image.getWidth(), image.getHeight());
                if (mousePos != null && clickbox.contains(mousePos.getX(), mousePos.getY())) {
                    if (!Strings.isNullOrEmpty(worldPoint.getTooltip())) {
                        tooltipPoint = worldPoint;
                    }

                    if (worldPoint.isJumpOnClick()) {
                        assert worldPoint.getName() != null;
                        hoveredPoint = worldPoint;
                    }
                }
            }
        }

        final Widget rsTooltip = client.getWidget(ComponentID.WORLD_MAP_TOOLTIP);
        if (rsTooltip != null) {
            rsTooltip.setHidden(tooltipPoint != null);
        }

        return null;
    }

    /**
     * Get the screen coordinates for a WorldPoint on the world map
     *
     * @param worldPoint WorldPoint to get screen coordinates of
     * @return Point of screen coordinates of the center of the world point
     */
    public Point mapWorldPointToGraphicsPoint(WorldPoint worldPoint) {
        WorldMap worldMap = client.getWorldMap();

        if (!worldMap.getWorldMapData().surfaceContainsPosition(worldPoint.getX(), worldPoint.getY())) {
            return null;
        }

        float pixelsPerTile = worldMap.getWorldMapZoom();

        Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (map != null) {
            Rectangle worldMapRect = map.getBounds();

            int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
            int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

            Point worldMapPosition = worldMap.getWorldMapPosition();

            //Offset in tiles from anchor sides
            int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
            int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
            int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

            int xGraphDiff = ((int) (xTileOffset * pixelsPerTile));
            int yGraphDiff = (int) (yTileOffset * pixelsPerTile);

            //Center on tile.
            yGraphDiff -= pixelsPerTile - Math.ceil(pixelsPerTile / 2);
            xGraphDiff += pixelsPerTile - Math.ceil(pixelsPerTile / 2);

            yGraphDiff = worldMapRect.height - yGraphDiff;
            yGraphDiff += (int) worldMapRect.getY();
            xGraphDiff += (int) worldMapRect.getX();

            return new Point(xGraphDiff, yGraphDiff);
        }
        return null;
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

/*    private void drawTooltip(Graphics2D graphics, WorldMapPoint worldPoint) {
        String tooltip = worldPoint.getTooltip();
        Point drawPoint = mapWorldPointToGraphicsPoint(worldPoint.getWorldPoint());
        if (tooltip == null || tooltip.length() <= 0 || drawPoint == null) {
            return;
        }

        List<String> rows = TOOLTIP_SPLITTER.splitToList(tooltip);

        if (rows.isEmpty()) {
            return;
        }

        drawPoint = new Point(drawPoint.getX() + TOOLTIP_OFFSET_WIDTH, drawPoint.getY() + TOOLTIP_OFFSET_HEIGHT);

        final Rectangle bounds = new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
        final Shape mapArea = getWorldMapClipArea(bounds);
        graphics.setClip(mapArea);
        graphics.setColor(JagexColors.TOOLTIP_BACKGROUND);
        graphics.setFont(FontManager.getRunescapeFont());
        FontMetrics fm = graphics.getFontMetrics();
        int width = rows.stream().map(fm::stringWidth).max(Integer::compareTo).get();
        int height = fm.getHeight();

        Rectangle tooltipRect = new Rectangle(drawPoint.getX() - TOOLTIP_PADDING_WIDTH, drawPoint.getY() - TOOLTIP_PADDING_HEIGHT, width + TOOLTIP_PADDING_WIDTH * 2, height * rows.size() + TOOLTIP_PADDING_HEIGHT * 2);
        graphics.fillRect((int) tooltipRect.getX(), (int) tooltipRect.getY(), (int) tooltipRect.getWidth(), (int) tooltipRect.getHeight());

        graphics.setColor(JagexColors.TOOLTIP_BORDER);
        graphics.drawRect((int) tooltipRect.getX(), (int) tooltipRect.getY(), (int) tooltipRect.getWidth(), (int) tooltipRect.getHeight());

        graphics.setColor(JagexColors.TOOLTIP_TEXT);
        for (int i = 0; i < rows.size(); i++) {
            graphics.drawString(rows.get(i), drawPoint.getX(), drawPoint.getY() + TOOLTIP_TEXT_OFFSET_HEIGHT + (i + 1) * height);
        }
    }*/

    private Point clipToRectangle(Point drawPoint, Rectangle mapDisplayRectangle) {
        int clippedX = drawPoint.getX();

        if (drawPoint.getX() < mapDisplayRectangle.getX()) {
            clippedX = (int) mapDisplayRectangle.getX();
        }

        if (drawPoint.getX() > mapDisplayRectangle.getX() + mapDisplayRectangle.getWidth()) {
            clippedX = (int) (mapDisplayRectangle.getX() + mapDisplayRectangle.getWidth());
        }

        int clippedY = drawPoint.getY();

        if (drawPoint.getY() < mapDisplayRectangle.getY()) {
            clippedY = (int) mapDisplayRectangle.getY();
        }

        if (drawPoint.getY() > mapDisplayRectangle.getY() + mapDisplayRectangle.getHeight()) {
            clippedY = (int) (mapDisplayRectangle.getY() + mapDisplayRectangle.getHeight());
        }

        return new Point(clippedX, clippedY);
    }
}
