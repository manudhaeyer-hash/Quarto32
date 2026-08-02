package com.codingame.game.engine;

public class Piece {
    public final int id;
    public final boolean isTall;
    public final boolean isDark;
    public final boolean isSquare;
    public final boolean isHollow;

    public final boolean isGlowing;
    public int placedBy = -1;

    public Piece(int id) {
        this.id = id;
        this.isGlowing = (id & 16) != 0;
        this.isTall = (id & 8) != 0;
        this.isDark = (id & 4) != 0;
        this.isSquare = (id & 2) != 0;
        this.isHollow = (id & 1) != 0;
    }
    
    public static boolean shareCharacteristic(Piece a, Piece b, Piece c, Piece d) {
        if (a == null || b == null || c == null || d == null) return false;
        
        if (a.isGlowing == b.isGlowing && b.isGlowing == c.isGlowing && c.isGlowing == d.isGlowing) return true;
        if (a.isTall == b.isTall && b.isTall == c.isTall && c.isTall == d.isTall) return true;
        if (a.isDark == b.isDark && b.isDark == c.isDark && c.isDark == d.isDark) return true;
        if (a.isSquare == b.isSquare && b.isSquare == c.isSquare && c.isSquare == d.isSquare) return true;
        if (a.isHollow == b.isHollow && b.isHollow == c.isHollow && c.isHollow == d.isHollow) return true;
        
        return false;
    }
}
