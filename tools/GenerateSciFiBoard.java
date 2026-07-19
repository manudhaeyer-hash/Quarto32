import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateSciFiBoard {
    private static double getIsoX(double x, double y) { return 960 + (x - y) * 110; }
    private static double getIsoY(double x, double y) { return 540 + (x + y - 5) * 55 - 30; }

    public static void main(String[] args) throws Exception {
        int width = 1920;
        int height = 1080;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        double padding = 0.5;
        double[][] gridPts = {
            {1 - padding, 0 - padding},
            {4 + padding, 0 - padding},
            {5 + padding, 1 - padding},
            {5 + padding, 4 + padding},
            {4 + padding, 5 + padding},
            {1 - padding, 5 + padding},
            {0 - padding, 4 + padding},
            {0 - padding, 1 - padding}
        };

        double[] px = new double[8];
        double[] py = new double[8];
        for (int i = 0; i < 8; i++) {
            px[i] = getIsoX(gridPts[i][0], gridPts[i][1]);
            py[i] = getIsoY(gridPts[i][0], gridPts[i][1]) - 30;
        }

        int thickness = 40; 

        // Shadow
        Path2D shadow = new Path2D.Double();
        shadow.moveTo(px[0], py[0] + thickness + 15);
        for (int i = 1; i < 8; i++) shadow.lineTo(px[i], py[i] + thickness + (i>1 && i<6 ? 35 : 15));
        shadow.closePath();
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fill(shadow);

        // Edges
        for (int i = 0; i < 8; i++) {
            int next = (i + 1) % 8;
            // Only draw visible edges (left-facing or right-facing or bottom-facing)
            if (i >= 3 && i <= 6) {
                Path2D edge = new Path2D.Double();
                edge.moveTo(px[i], py[i]);
                edge.lineTo(px[next], py[next]);
                edge.lineTo(px[next], py[next] + thickness);
                edge.lineTo(px[i], py[i] + thickness);
                edge.closePath();
                g2d.setPaint(new GradientPaint((float)px[i], (float)py[i], new Color(30, 35, 45), (float)px[next], (float)py[next], new Color(15, 20, 25)));
                g2d.fill(edge);
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.draw(edge);
            }
        }

        Path2D board = new Path2D.Double();
        board.moveTo(px[0], py[0]);
        for (int i = 1; i < 8; i++) board.lineTo(px[i], py[i]);
        board.closePath();
        
        GradientPaint topPaint = new GradientPaint((float)px[0], (float)py[0], new Color(60, 70, 80), (float)px[4], (float)py[4], new Color(40, 45, 55));
        g2d.setPaint(topPaint);
        g2d.fill(board);

        g2d.setStroke(new BasicStroke(4));
        g2d.setColor(new Color(0, 150, 255, 100)); // Blue glow
        g2d.draw(board);
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(new Color(150, 220, 255)); // Bright core
        g2d.draw(board);

        Area boardArea = new Area(board);

        for(int y=0; y<6; y++) {
            for(int x=0; x<6; x++) {
                if ((x==0 && y==0) || (x==0 && y==5) || (x==5 && y==0) || (x==5 && y==5)) continue;

                double cx = getIsoX(x, y);
                double cy = getIsoY(x, y) - 30;
                
                double rx = 53;
                double ry = 26.5;
                
                Ellipse2D slotOuter = new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2);
                Ellipse2D slotInner = new Ellipse2D.Double(cx - rx + 4, cy - ry + 4, (rx - 4) * 2, (ry - 4) * 2);

                g2d.setColor(new Color(10, 15, 20, 200)); 
                g2d.fill(slotOuter);
                
                g2d.setColor(new Color(20, 30, 40));
                g2d.fill(slotInner);
                
                g2d.setStroke(new BasicStroke(3));
                g2d.setColor(new Color(0, 150, 255, 80)); 
                g2d.drawArc((int)(cx - rx + 1), (int)(cy - ry + 1), (int)(rx*2 - 2), (int)(ry*2 - 2), 0, 360);
                
                g2d.setStroke(new BasicStroke(1));
                g2d.setColor(new Color(100, 200, 255, 180)); 
                g2d.drawArc((int)(cx - rx + 3), (int)(cy - ry + 3), (int)(rx*2 - 6), (int)(ry*2 - 6), 0, 360);
                
                // --- Generate Rim Mask ---
                BufferedImage rimImg = new BufferedImage(140, 80, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gRim = rimImg.createGraphics();
                gRim.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gRim.translate(70 - cx, 40 - cy); // Center the mask locally

                Area rect = new Area(new Rectangle2D.Double(cx - 70, cy, 140, 80));
                rect.intersect(boardArea);
                Area hole = new Area(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
                rect.subtract(hole);
                
                gRim.setPaint(topPaint);
                gRim.fill(rect);

                // Redraw board edge strokes to prevent the mask from thinning them
                gRim.setStroke(new BasicStroke(4));
                gRim.setColor(new Color(0, 150, 255, 100)); 
                gRim.draw(board);
                gRim.setStroke(new BasicStroke(2));
                gRim.setColor(new Color(150, 220, 255)); 
                gRim.draw(board);

                gRim.setStroke(new BasicStroke(3));
                gRim.setColor(new Color(0, 150, 255, 80)); 
                gRim.drawArc((int)(cx - rx + 1), (int)(cy - ry + 1), (int)(rx*2 - 2), (int)(ry*2 - 2), 180, 180); // bottom half only
                
                gRim.setStroke(new BasicStroke(1));
                gRim.setColor(new Color(100, 200, 255, 180)); 
                gRim.drawArc((int)(cx - rx + 3), (int)(cy - ry + 3), (int)(rx*2 - 6), (int)(ry*2 - 6), 180, 180);
                
                gRim.dispose();
                ImageIO.write(rimImg, "png", new File("C:/AGrav/Quarto/Quarto/src/main/resources/view/assets/rim_" + x + "_" + y + ".png"));
            }
        }
        
        // --- Draw coordinates on edges ---
        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2d.setColor(new Color(150, 220, 255, 120)); // discrete light blue
        FontMetrics fm = g2d.getFontMetrics();

        // x labels (1 to 5) - left edge
        for (int x = 1; x <= 5; x++) {
            double cx, cy;
            if (x == 5) { cx = getIsoX(5, 5); cy = getIsoY(5, 5) - 30; }
            else { cx = getIsoX(x, 5.5); cy = getIsoY(x, 5.5) - 30; }
            
            String s = String.valueOf(x);
            int w = fm.stringWidth(s);
            
            g2d.drawString(s, (float)(cx - w/2.0f), (float)(cy + thickness / 2.0 + 8));
        }

        // y labels (1 to 4) - right edge
        for (int y = 1; y <= 4; y++) {
            double cx, cy;
            cx = getIsoX(5.5, y); cy = getIsoY(5.5, y) - 30;
            
            String s = String.valueOf(y);
            int w = fm.stringWidth(s);
            
            g2d.drawString(s, (float)(cx - w/2.0f), (float)(cy + thickness / 2.0 + 8));
        }
        
        g2d.dispose();
        ImageIO.write(img, "png", new File("C:/AGrav/Quarto/Quarto/src/main/resources/view/assets/sci_fi_board_iso.png"));
        System.out.println("32-cell SciFi board generated");
    }
}
