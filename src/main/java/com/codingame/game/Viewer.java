package com.codingame.game;

import java.util.HashMap;
import java.util.Map;

import com.codingame.game.engine.Board;
import com.codingame.game.engine.Piece;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.Curve;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Group;
import com.codingame.gameengine.module.entities.Text;
import com.codingame.gameengine.module.entities.Rectangle;
import com.codingame.gameengine.module.entities.Polygon;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.tooltip.TooltipModule;

public class Viewer {

    public static final int COLOR_BG = 0x050A10; // Dark grid bg
    public static final int COLOR_TEXT = 0x00E5FF; // Neon Cyan
    public static final int[] PLAYER_COLORS = { 0xFF7800, 0x0096FF }; // Swap colors to match console
    // Traditional wooden pieces
    static final int COLOR_PIECE_LIGHT = 0xE6A861; // Light oak
    static final int COLOR_PIECE_DARK = 0x662C27;  // Dark mahogany

    private final GraphicEntityModule g;
    private final TooltipModule tooltipModule;
    private final MultiplayerGameManager<Player> gm;
    
    private Map<Integer, Group> pieceGroups = new HashMap<>();
    private Rectangle[] highlights = new Rectangle[2];
    private Text[] lastMoveTexts = new Text[2];
    
    private Group[] playerHuds = new Group[2];
    private Sprite[] bottomFrames = new Sprite[2];
    private Text[] bottomTexts = new Text[2];
    
    private Polygon[][] gridPolygons = new Polygon[6][6];
    
    public Viewer(GraphicEntityModule g, TooltipModule tooltipModule, MultiplayerGameManager<Player> gm) {
        this.g = g;
        this.tooltipModule = tooltipModule;
        this.gm = gm;
        
        for (int i = 0; i < 32; i++) {
            pieceGroups.put(i, createPieceGroup(i));
        }
    }

    private Group createPieceGroup(int id) {
        Piece p = new Piece(id);
        Group group = g.createGroup();
        group.add(g.createSprite().setImage("piece_" + id + ".png")
                .setAnchorX(0.5).setAnchorY(0.8)
                .setBaseWidth(180).setBaseHeight(180)
                .setX(0).setY(0));
        
        group.setX(-1000).setY(-1000);
        return group;
    }

    // Isometric mathematical layout
    private int getIsoX(double x, double y) { return (int)(960 + (x - y) * 110); }
    private int getIsoY(double x, double y) { return (int)(540 + (x + y - 5) * 55 - 30); }

    public void drawBackground() {
        g.createRectangle().setX(0).setY(0).setWidth(1920).setHeight(1080)
                .setFillColor(COLOR_BG).setZIndex(-30);
                
        // Background matrix grid (Sober)
        for (int i = 0; i < 40; i++) {
            g.createRectangle().setX(0).setY(i*27).setWidth(1920).setHeight(1).setFillColor(COLOR_TEXT).setAlpha(0.08).setZIndex(-29);
            g.createRectangle().setX(i*48).setY(0).setWidth(1).setHeight(1080).setFillColor(COLOR_TEXT).setAlpha(0.08).setZIndex(-29);
        }

        g.createSprite().setImage("sci_fi_board_iso.png")
            .setX(0).setY(0)
            .setZIndex(-20);

        for (int i = 0; i < gm.getPlayerCount(); i++) {
            Player player = gm.getPlayer(i);
            
            int pX = i == 0 ? 20 : 1450;
            playerHuds[i] = g.createGroup().setX(pX).setY(20).setZIndex(-6);
            
            playerHuds[i].add(g.createSprite().setImage("hud_player_frame.png")
                .setX(0).setY(0).setBaseWidth(450).setBaseHeight(130));
            
            highlights[i] = g.createRectangle().setX(0).setY(0).setWidth(450).setHeight(130)
                .setFillColor(PLAYER_COLORS[i]).setAlpha(0);
            playerHuds[i].add(highlights[i]);
                
            playerHuds[i].add(g.createSprite().setImage(player.getAvatarToken())
                    .setX(30).setY(15).setBaseWidth(80).setBaseHeight(80));
            playerHuds[i].add(g.createText(player.getNicknameToken())
                    .setX(150).setY(25).setFontSize(36).setFillColor(PLAYER_COLORS[i])
                    .setFontFamily("monospace"));
                    
            lastMoveTexts[i] = g.createText("WAITING...")
                    .setX(150).setY(75).setFontSize(36).setFillColor(0xFFFFFF).setAlpha(0.8)
                    .setFontFamily("monospace");
            playerHuds[i].add(lastMoveTexts[i]);
        }
        

        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                if ((x==0 && y==0) || (x==0 && y==5) || (x==5 && y==0) || (x==5 && y==5)) continue;
                
                // Invisible polygon for interaction
                Polygon poly = g.createPolygon()
                    .addPoint(getIsoX(x, y - 0.5), getIsoY(x, y - 0.5))
                    .addPoint(getIsoX(x + 0.5, y), getIsoY(x + 0.5, y))
                    .addPoint(getIsoX(x, y + 0.5), getIsoY(x, y + 0.5))
                    .addPoint(getIsoX(x - 0.5, y), getIsoY(x - 0.5, y))
                    .setFillColor(0x0B132B).setAlpha(0.01)
                    .setLineColor(COLOR_TEXT).setLineWidth(3).setAlpha(0.01);
                    
                gridPolygons[x][y] = poly;
                tooltipModule.setTooltipText(poly, "(" + x + ", " + y + ")");
                    
                // Front rim mask to obscure submerged piece base
                g.createSprite().setImage("rim_" + x + "_" + y + ".png")
                    .setX(getIsoX(x, y) - 70)
                    .setY(getIsoY(x, y) - 30 - 40)
                    .setZIndex(20 + (x + y) * 2 + 1);
            }
        }
        
        // UI Frame for Available Pieces (Bottom Left Extremity)
        bottomFrames[0] = g.createSprite().setImage("hud_frame.png")
            .setX(20).setY(700).setBaseWidth(450).setBaseHeight(360).setZIndex(-6);
            
        bottomTexts[0] = g.createText("AVAILABLE PIECES")
            .setX(245).setY(740).setAnchor(0.5).setFontSize(26).setFillColor(COLOR_TEXT).setFontFamily("monospace").setZIndex(-5);
            
        // UI Frame for Piece to Place (Bottom Right Extremity)
        bottomFrames[1] = g.createSprite().setImage("hud_frame.png")
            .setX(1450).setY(700).setBaseWidth(450).setBaseHeight(360).setZIndex(-6);
            
        bottomTexts[1] = g.createText("PIECE TO PLACE")
            .setX(1675).setY(740).setAnchor(0.5).setFontSize(26).setFillColor(COLOR_TEXT).setFontFamily("monospace").setZIndex(-5);
            
        // The frames for available pieces and piece to place are now drawn on the background image.
    }

    public void update(Board board, int pieceToPlace, int activePlayerId, String lastMove) {
        if (activePlayerId >= 0) {
            if (highlights.length > 0 && highlights[0] != null) highlights[0].setAlpha(activePlayerId == 0 ? 0.2 : 0);
            if (highlights.length > 1 && highlights[1] != null) highlights[1].setAlpha(activePlayerId == 1 ? 0.2 : 0);
            if (activePlayerId < lastMoveTexts.length && lastMoveTexts[activePlayerId] != null && lastMove != null && !lastMove.isEmpty()) {
                lastMoveTexts[activePlayerId].setText(lastMove);
            }
        } else {
            if (highlights.length > 0 && highlights[0] != null) highlights[0].setAlpha(0);
            if (highlights.length > 1 && highlights[1] != null) highlights[1].setAlpha(0);
        }
        
        for (int i = 0; i < 32; i++) {
            Group group = pieceGroups.get(i);
            if (i == pieceToPlace) {
                group.setX(1675, Curve.EASE_IN_AND_OUT).setY(966, Curve.EASE_IN_AND_OUT).setScale(1.6).setZIndex(100);
                group.setAlpha(1.0);
            } else if (board.getAvailablePieces().contains(i)) {
                int col = i % 8;
                int row = i / 8;
                double ccol = col - 3.5;
                double crow = row - 1.5;
                
                int ax = 245 + (int)((ccol - crow) * 35);
                int ay = 930 + (int)((ccol + crow) * 18);
                group.setX(ax, Curve.EASE_IN_AND_OUT).setY(ay, Curve.EASE_IN_AND_OUT).setScale(0.5).setZIndex(50 + col + row);
                group.setAlpha(1.0);
            } else {
                boolean onBoard = false;
                for (int y = 0; y < 6; y++) {
                    for (int x = 0; x < 6; x++) {
                        if ((x==0 && y==0) || (x==0 && y==5) || (x==5 && y==0) || (x==5 && y==5)) continue;
                        if (board.getPieceId(x, y) == i) {
                            group.setX(getIsoX(x, y), Curve.EASE_IN_AND_OUT)
                                 .setY(getIsoY(x, y) - 20, Curve.EASE_IN_AND_OUT) // lower than center for deep sinking effect
                                 .setScale(1.0)
                                 .setZIndex(20 + (x + y) * 2);
                            group.setAlpha(1.0);
                            onBoard = true;
                            
                            // Update Tooltip
                            com.codingame.game.engine.Piece p = board.getPiece(x, y);
                            String tooltip = "(" + x + ", " + y + ")\nPiece: " + i;
                            if (p != null && p.placedBy != -1) {
                                tooltip += "\nPlaced by: Player " + p.placedBy;
                            }
                            tooltipModule.setTooltipText(gridPolygons[x][y], tooltip);
                        }
                    }
                }
                if (!onBoard) group.setAlpha(0.0); // Hide if not available, not pieceToPlace, and not on board (failsafe)
            }
        }
    }
    
    public void drawWinningLine(int[] lineCoords) {
        g.commitWorldState(0.2); // Force all pieces to finish moving quickly (20% of 3000ms = 600ms)

        if (lineCoords == null || lineCoords.length < 9) return;
        
        int type = lineCoords[0];
        if (type == 0) {
            int x1 = getIsoX(lineCoords[1], lineCoords[2]);
            int y1 = getIsoY(lineCoords[1], lineCoords[2]);
            
            int x2 = getIsoX(lineCoords[7], lineCoords[8]);
            int y2 = getIsoY(lineCoords[7], lineCoords[8]);
            
            com.codingame.gameengine.module.entities.Line l1 = g.createLine().setX(x1).setY(y1 - 30).setX2(x2).setY2(y2 - 30)
                .setLineColor(0xFFFFFF).setLineWidth(20).setAlpha(0).setZIndex(100);
            com.codingame.gameengine.module.entities.Line l2 = g.createLine().setX(x1).setY(y1 - 30).setX2(x2).setY2(y2 - 30)
                .setLineColor(COLOR_TEXT).setLineWidth(40).setAlpha(0).setZIndex(99);
                
            g.commitEntityState(0.2, l1, l2); // Stay invisible until 0.2
            l1.setAlpha(0.8);
            l2.setAlpha(0.4);
            g.commitEntityState(0.5, l1, l2); // Fade in quickly from 0.2 to 0.5. Then static pause until 1.0
        } else if (type == 1) {
            // It's a 2x2 square
            // We draw a polygon over the 4 pieces
            int topX = getIsoX(lineCoords[1] - 0.5, lineCoords[2] - 0.5);
            int topY = getIsoY(lineCoords[1] - 0.5, lineCoords[2] - 0.5);
            int rightX = getIsoX(lineCoords[3] + 0.5, lineCoords[4] - 0.5);
            int rightY = getIsoY(lineCoords[3] + 0.5, lineCoords[4] - 0.5);
            int bottomX = getIsoX(lineCoords[7] + 0.5, lineCoords[8] + 0.5);
            int bottomY = getIsoY(lineCoords[7] + 0.5, lineCoords[8] + 0.5);
            int leftX = getIsoX(lineCoords[5] - 0.5, lineCoords[6] + 0.5);
            int leftY = getIsoY(lineCoords[5] - 0.5, lineCoords[6] + 0.5);

            Polygon p = g.createPolygon()
                .addPoint(topX, topY - 30)
                .addPoint(rightX, rightY - 30)
                .addPoint(bottomX, bottomY - 30)
                .addPoint(leftX, leftY - 30)
                .setFillColor(COLOR_TEXT).setAlpha(0).setZIndex(99)
                .setLineColor(0xFFFFFF).setLineWidth(10).setLineAlpha(0);
                
            g.commitEntityState(0.2, p);
            p.setAlpha(0.3);
            p.setLineAlpha(1.0);
            g.commitEntityState(0.5, p);
        }
    }
    
    public void drawEndScreen(String[] endTexts, Board board) {
        int winnerIndex = -1;
        for (int i = 0; i < 2; i++) {
            lastMoveTexts[i].setText(endTexts[i]);
            if (endTexts[i].startsWith("Winner")) {
                winnerIndex = i;
                lastMoveTexts[i].setFillColor(0xFFD700).setFontSize(36).setY(70);
            } else if (endTexts[i].startsWith("Loser") || endTexts[i].equals("Eliminated")) {
                lastMoveTexts[i].setFillColor(0x888888).setFontSize(26).setY(75);
            } else { // Draw
                lastMoveTexts[i].setFillColor(0xAAAAAA).setFontSize(30).setY(70);
            }
        }
        
        // Move players
        if (winnerIndex == 1) { // Player 1 (Right) won
            playerHuds[1].setX(20, Curve.EASE_IN_AND_OUT).setY(20, Curve.EASE_IN_AND_OUT);
            playerHuds[0].setX(20, Curve.EASE_IN_AND_OUT).setY(160, Curve.EASE_IN_AND_OUT);
        } else { // Player 0 won, or Draw
            playerHuds[0].setX(20, Curve.EASE_IN_AND_OUT).setY(20, Curve.EASE_IN_AND_OUT);
            playerHuds[1].setX(20, Curve.EASE_IN_AND_OUT).setY(160, Curve.EASE_IN_AND_OUT);
        }
        
        for (int i=0; i<2; i++) {
            bottomFrames[i].setAlpha(0);
            bottomTexts[i].setAlpha(0);
        }
        
        for (int i = 0; i < 32; i++) {
            Group piece = pieceGroups.get(i);
            // Only hide pieces that are still available or the one currently to place
            if (board.getAvailablePieces().contains(i) || piece.getY() > 900) {
                piece.setAlpha(0);
                g.commitEntityState(0.5, piece); // 2 seconds out of 4 seconds frame = 0.5
            }
        }
        
        g.commitEntityState(0.5, playerHuds[0], playerHuds[1], bottomFrames[0], bottomFrames[1], bottomTexts[0], bottomTexts[1], lastMoveTexts[0], lastMoveTexts[1]);
        
        // Logo appears after 3s (0.75), fades in to 4s (1.0)
        Sprite logo = g.createSprite().setImage("logo.png")
            .setAnchor(0.5).setX(960).setY(400).setScale(1).setAlpha(0).setZIndex(1000);
            
        g.commitEntityState(0.75, logo); // Hidden until 3s
        
        logo.setAlpha(1.0);
        g.commitEntityState(1.0, logo); // Fades in completely by the end
    }

    public void drawStartCinematic() {
        com.codingame.gameengine.module.entities.Sprite logo = g.createSprite().setImage("logo.png")
            .setAnchor(0.5).setX(960).setY(400).setScale(0).setAlpha(0).setZIndex(1000);
            
        g.commitEntityState(0.0, logo);
        
        logo.setScale(1.0).setAlpha(1.0);
        g.commitEntityState(0.3, logo);
        
        logo.setAlpha(0.0);
        g.commitEntityState(1.0, logo);
    }
}

