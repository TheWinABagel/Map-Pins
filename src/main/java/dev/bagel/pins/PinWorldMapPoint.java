package dev.bagel.pins;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

import java.awt.*;
import java.awt.image.BufferedImage;

@SuperBuilder
public final class PinWorldMapPoint extends WorldMapPoint {
    private PinPoint basePoint;
    public PinWorldMapPoint(PinPoint basePoint) {
        super(basePoint.toWorldPoint(), changeColor(basePoint.getStyle().getImage(), basePoint.getStyle().isCanBeColored() ? basePoint.getColor() : null));
        this.setName("Pin");
        this.setTarget(this.getWorldPoint());
        this.setJumpOnClick(true);
        this.setSnapToEdge(true);
        this.basePoint = basePoint;
    }


    public boolean doesWorldPointMatch(WorldPoint worldPoint) {
        WorldArea thisPos = new WorldArea(basePoint.getRegionX(), basePoint.getRegionY(), basePoint.getStyle().getX(), basePoint.getStyle().getY(), basePoint.getZ());
        WorldArea otherPos = new WorldArea(worldPoint.getRegionX(), worldPoint.getRegionY(), 5, 5, worldPoint.getPlane());
        return thisPos.intersectsWith(worldPoint.toWorldArea());
    }

/*    public static PinWorldMapPoint getFirstPin() {
        return
    }*/

    private static BufferedImage changeColor(BufferedImage image, Color replacement_color) {
        if (replacement_color == null) {
            return image;
        }
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

}
