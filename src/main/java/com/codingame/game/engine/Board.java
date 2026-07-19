package com.codingame.game.engine;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private Piece[][] grid;
    private List<Integer> availablePieces;

    public Board() {
        grid = new Piece[6][6];
        availablePieces = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            availablePieces.add(i);
        }
    }

    public boolean isCorner(int x, int y) {
        return (x == 0 && y == 0) || (x == 0 && y == 5) || (x == 5 && y == 0) || (x == 5 && y == 5);
    }

    public Piece getPiece(int x, int y) {
        if (x < 0 || x > 5 || y < 0 || y > 5 || isCorner(x, y)) return null;
        return grid[y][x];
    }
    
    public int getPieceId(int x, int y) {
        Piece p = getPiece(x, y);
        return p == null ? -1 : p.id;
    }

    public List<Integer> getAvailablePieces() {
        return availablePieces;
    }

    public void choosePiece(int pieceId) throws IllegalArgumentException {
        if (!availablePieces.contains(pieceId)) {
            throw new IllegalArgumentException("Piece not available: " + pieceId);
        }
        availablePieces.remove((Integer)pieceId);
    }

    public void placePiece(int x, int y, int pieceId) throws IllegalArgumentException {
        if (x < 0 || x > 5 || y < 0 || y > 5) {
            throw new IllegalArgumentException("Coordinates out of bounds: " + x + ", " + y);
        }
        if (isCorner(x, y)) {
            throw new IllegalArgumentException("Cannot place piece on a corner: " + x + ", " + y);
        }
        if (grid[y][x] != null) {
            throw new IllegalArgumentException("Cell already occupied: " + x + ", " + y);
        }
        grid[y][x] = new Piece(pieceId);
    }

    public int[] getWinningLine() {
        // Return format: {type (0=line, 1=square), x1, y1, x2, y2, x3, y3, x4, y4}
        
        // Rows
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x <= 2; x++) {
                if (Piece.shareCharacteristic(getPiece(x, y), getPiece(x+1, y), getPiece(x+2, y), getPiece(x+3, y))) {
                    return new int[]{0, x, y, x+1, y, x+2, y, x+3, y};
                }
            }
        }
        // Columns
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y <= 2; y++) {
                if (Piece.shareCharacteristic(getPiece(x, y), getPiece(x, y+1), getPiece(x, y+2), getPiece(x, y+3))) {
                    return new int[]{0, x, y, x, y+1, x, y+2, x, y+3};
                }
            }
        }
        // Diagonals (top-left to bottom-right)
        for (int y = 0; y <= 2; y++) {
            for (int x = 0; x <= 2; x++) {
                if (Piece.shareCharacteristic(getPiece(x, y), getPiece(x+1, y+1), getPiece(x+2, y+2), getPiece(x+3, y+3))) {
                    return new int[]{0, x, y, x+1, y+1, x+2, y+2, x+3, y+3};
                }
            }
        }
        // Diagonals (top-right to bottom-left)
        for (int y = 0; y <= 2; y++) {
            for (int x = 3; x <= 5; x++) {
                if (Piece.shareCharacteristic(getPiece(x, y), getPiece(x-1, y+1), getPiece(x-2, y+2), getPiece(x-3, y+3))) {
                    return new int[]{0, x, y, x-1, y+1, x-2, y+2, x-3, y+3};
                }
            }
        }
        // 2x2 Squares
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 4; x++) {
                if (Piece.shareCharacteristic(getPiece(x, y), getPiece(x+1, y), getPiece(x, y+1), getPiece(x+1, y+1))) {
                    return new int[]{1, x, y, x+1, y, x, y+1, x+1, y+1};
                }
            }
        }
        
        return null;
    }
}
