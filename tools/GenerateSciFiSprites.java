import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateSciFiSprites {
    public static void main(String[] args) throws Exception {
        for(int i=0; i<32; i++) {
            boolean isGlowing = (i & 16) != 0;
            boolean isTall = (i & 8) != 0; // Fixed: using positive logic to match Piece.java
            boolean isDark = (i & 4) != 0; 
            boolean isSquare = (i & 2) != 0;
            boolean isHollow = (i & 1) != 0;

            int width = 300;
            int height = 300;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color baseColor = isDark ? new Color(0, 150, 255) : new Color(255, 120, 0); 
            Color transColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 120); 
            Color edgeColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 255); 
            Color brightColor = new Color(
                Math.min(255, baseColor.getRed() + 100), 
                Math.min(255, baseColor.getGreen() + 100), 
                Math.min(255, baseColor.getBlue() + 100)
            );

            int cx = 150;
            int cy = 250; 
            int rx = 48;
            int ry = 24;
            int h = isTall ? 100 : 50;

            if (isSquare) {
                Path2D leftFace = new Path2D.Double();
                leftFace.moveTo(cx, cy);
                leftFace.lineTo(cx - rx, cy - ry);
                leftFace.lineTo(cx - rx, cy - ry - h);
                leftFace.lineTo(cx, cy - h);
                leftFace.closePath();
                
                Path2D rightFace = new Path2D.Double();
                rightFace.moveTo(cx, cy);
                rightFace.lineTo(cx + rx, cy - ry);
                rightFace.lineTo(cx + rx, cy - ry - h);
                rightFace.lineTo(cx, cy - h);
                rightFace.closePath();
                
                Path2D topFace = new Path2D.Double();
                topFace.moveTo(cx, cy - h);
                topFace.lineTo(cx - rx, cy - ry - h);
                topFace.lineTo(cx, cy - ry*2 - h);
                topFace.lineTo(cx + rx, cy - ry - h);
                topFace.closePath();

                g2d.setPaint(new GradientPaint(cx-rx, cy-ry, transColor, cx, cy, new Color(0,0,0,100)));
                g2d.fill(leftFace);
                g2d.setPaint(new GradientPaint(cx+rx, cy-ry, transColor, cx, cy, new Color(0,0,0,50)));
                g2d.fill(rightFace);
                g2d.setColor(new Color(brightColor.getRed(), brightColor.getGreen(), brightColor.getBlue(), 180));
                g2d.fill(topFace);

                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(edgeColor);
                g2d.draw(leftFace);
                g2d.draw(rightFace);
                g2d.draw(topFace);

                if (isHollow) {
                    Path2D hole = new Path2D.Double();
                    int hrx = 24;
                    int hry = 12;
                    hole.moveTo(cx, cy - h - hry); 
                    hole.lineTo(cx - hrx, cy - ry - h - hry);
                    hole.lineTo(cx, cy - ry*2 - h - hry);
                    hole.lineTo(cx + hrx, cy - ry - h - hry);
                    hole.closePath();
                    g2d.setColor(new Color(0, 0, 0, 200));
                    g2d.fill(hole);
                    g2d.setColor(brightColor);
                    g2d.draw(hole);
                } else {
                    g2d.setColor(new Color(255, 255, 255, 100));
                    g2d.fill(topFace);
                }
                
            } else {
                Rectangle2D rect = new Rectangle2D.Double(cx - rx, cy - ry - h, rx * 2, h);
                g2d.setPaint(new GradientPaint(cx-rx, 0, transColor, cx+rx, 0, new Color(0,0,0,100)));
                g2d.fill(rect);

                Ellipse2D botEllipse = new Ellipse2D.Double(cx - rx, cy - ry * 2, rx * 2, ry * 2);
                g2d.fill(botEllipse); 
                
                Ellipse2D topEllipse = new Ellipse2D.Double(cx - rx, cy - ry * 2 - h, rx * 2, ry * 2);
                g2d.setColor(new Color(brightColor.getRed(), brightColor.getGreen(), brightColor.getBlue(), 180));
                g2d.fill(topEllipse);

                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(edgeColor);
                g2d.drawLine(cx - rx, cy - ry, cx - rx, cy - ry - h);
                g2d.drawLine(cx + rx, cy - ry, cx + rx, cy - ry - h);
                g2d.draw(topEllipse);
                g2d.drawArc((int)(cx - rx), (int)(cy - ry * 2), (int)(rx*2), (int)(ry*2), 180, 180);

                if (isHollow) {
                    Ellipse2D hole = new Ellipse2D.Double(cx - rx/2.0, cy - ry - h - ry/2.0, rx, ry);
                    g2d.setColor(new Color(0, 0, 0, 200));
                    g2d.fill(hole);
                    g2d.setColor(brightColor);
                    g2d.draw(hole);
                } else {
                    g2d.setColor(new Color(255, 255, 255, 100));
                    g2d.fill(topEllipse);
                }
            }

            // Draw glowing core if applicable
            if (isGlowing) {
                int coreY = cy - ry - (h / 2);
                Color glowCore = new Color(255, 255, 255, 255);
                Color glowAura = new Color(brightColor.getRed(), brightColor.getGreen(), brightColor.getBlue(), 0);
                
                RadialGradientPaint paint = new RadialGradientPaint(
                        new Point2D.Double(cx, coreY), 
                        30, 
                        new float[]{0.0f, 0.3f, 1.0f}, 
                        new Color[]{glowCore, brightColor, glowAura}
                );
                g2d.setPaint(paint);
                g2d.fillOval(cx - 30, coreY - 30, 60, 60);
            }

            g2d.dispose();
            ImageIO.write(img, "png", new File("C:/AGrav/Quarto/Quarto/src/main/resources/view/assets/piece_" + i + ".png"));
        }
        System.out.println("32 SciFi pieces generated");
    }
}
