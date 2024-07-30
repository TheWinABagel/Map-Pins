package dev.bagel;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

class PinMinimapOverlay extends Overlay {
    private final Client client;
    private final MapPinsConfig config;
    private final MapPinsPlugin plugin;

    @Inject
    private PinMinimapOverlay(Client client, MapPinsConfig config, MapPinsPlugin plugin) {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.drawPinOnMinimap()) {
            return null;
        }

/*
        final Collection<Pin> points = plugin.getPoints();
        for (final Pin point : points) {
            WorldPoint worldPoint = point.getPoint();
            if (worldPoint.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }

            Color tileColor = point.getColor();
            if (tileColor == null) {
                // If this is an old tile which has no color, use marker color
                tileColor = config.pinColor();
            }

            drawOnMinimap(graphics, worldPoint, tileColor);
        }*/

        return null;
    }

    private void drawOnMinimap(Graphics2D graphics, WorldPoint point, Color color) {
        if (!point.isInScene(client)) {
            return;
        }

        int x = point.getX() - client.getBaseX();
        int y = point.getY() - client.getBaseY();

        x <<= Perspective.LOCAL_COORD_BITS;
        y <<= Perspective.LOCAL_COORD_BITS;

        Point mp1 = Perspective.localToMinimap(client, new LocalPoint(x, y));
        Point mp2 = Perspective.localToMinimap(client, new LocalPoint(x, y + Perspective.LOCAL_TILE_SIZE));
        Point mp3 = Perspective.localToMinimap(client, new LocalPoint(x + Perspective.LOCAL_TILE_SIZE, y + Perspective.LOCAL_TILE_SIZE));
        Point mp4 = Perspective.localToMinimap(client, new LocalPoint(x + Perspective.LOCAL_TILE_SIZE, y));

        if (mp1 == null || mp2 == null || mp3 == null || mp4 == null) {
            return;
        }

        Polygon poly = new Polygon();
        poly.addPoint(mp1.getX(), mp1.getY());
        poly.addPoint(mp2.getX(), mp2.getY());
        poly.addPoint(mp3.getX(), mp3.getY());
        poly.addPoint(mp4.getX(), mp4.getY());

        Stroke stroke = new BasicStroke(1f);
        graphics.setStroke(stroke);
        graphics.setColor(color);
        graphics.drawPolygon(poly);
    }
}