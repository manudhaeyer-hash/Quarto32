package com.codingame.game;

import java.util.List;

import com.codingame.game.engine.Board;
import com.codingame.game.engine.Piece;
import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.MultiplayerGameManager;

import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;
import com.google.inject.Inject;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;

    @Inject private TooltipModule tooltipModule;

    private Board board;
    private int pieceToPlace = -1;
    private Viewer viewer;

    @Override
    public void init() {
        try {
            board = new Board();
            gameManager.setMaxTurns(32);
            gameManager.setFirstTurnMaxTime(1000);
            gameManager.setTurnMaxTime(50);
            gameManager.setFrameDuration(500);

            java.util.Random rnd = new java.util.Random(gameManager.getSeed());
            pieceToPlace = rnd.nextInt(32);
            board.choosePiece(pieceToPlace);

            for (Player player : gameManager.getActivePlayers()) {
                player.sendInputLine(String.valueOf(player.getIndex()));
            }

            viewer = new Viewer(graphicEntityModule, tooltipModule, gameManager);
            viewer.drawBackground();
            viewer.update(board, pieceToPlace, -1, null);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR IN INIT: " + e.getMessage());
            e.printStackTrace();
            throw e; // Rethrow to let the SDK wrapper crash properly with trace
        }
    }

    private void sendTurnInputs(Player player) {
        player.sendInputLine(String.valueOf(pieceToPlace));
        for (int y = 0; y < 6; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < 6; x++) {
                if (board.isCorner(x, y)) {
                    sb.append("-2 ");
                } else {
                    sb.append(board.getPieceId(x, y)).append(" ");
                }
            }
            player.sendInputLine(sb.toString().trim());
        }
        List<Integer> availablePieces = board.getAvailablePieces();
        player.sendInputLine(String.valueOf(availablePieces.size()));
        StringBuilder sb = new StringBuilder();
        for (int p : availablePieces) {
            sb.append(p).append(" ");
        }
        player.sendInputLine(availablePieces.isEmpty() ? "" : sb.toString().trim());
    }

    private int[] tieBreakerScore = new int[2];

    private int calculatePlacementScore(int x, int y, int pieceId) {
        int score = 0;
        Piece p = new Piece(pieceId);
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < 6 && ny >= 0 && ny < 6) {
                int nId = board.getPieceId(nx, ny);
                if (nId != -1) {
                    Piece n = new Piece(nId);
                    if (p.isGlowing == n.isGlowing) score++;
                    if (p.isTall == n.isTall) score++;
                    if (p.isDark == n.isDark) score++;
                    if (p.isSquare == n.isSquare) score++;
                    if (p.isHollow == n.isHollow) score++;
                }
            }
        }
        return score;
    }

    @Override
    public void gameTurn(int turn) {
        Player activePlayer = gameManager.getPlayer((turn - 1) % 2);

        try {
            sendTurnInputs(activePlayer);
            activePlayer.execute();

            String action = "";
            try {
                List<String> outputs = activePlayer.getOutputs();
                action = outputs.get(0).trim();
                String[] parts = action.split(" ");
                
                if (board.getAvailablePieces().isEmpty()) {
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("Expected <x> <y>");
                    }
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    board.placePiece(x, y, pieceToPlace);
                    tieBreakerScore[activePlayer.getIndex()] += calculatePlacementScore(x, y, pieceToPlace);
                    gameManager.addToGameSummary(activePlayer.getNicknameToken() + " placed piece " + pieceToPlace + " at " + x + " " + y);
                    pieceToPlace = -1;
                } else {
                    if (parts.length < 3) {
                        throw new IllegalArgumentException("Expected <x> <y> <id>");
                    }
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int id = Integer.parseInt(parts[2]);
                    
                    board.placePiece(x, y, pieceToPlace);
                    tieBreakerScore[activePlayer.getIndex()] += calculatePlacementScore(x, y, pieceToPlace);
                    gameManager.addToGameSummary(activePlayer.getNicknameToken() + " placed piece " + pieceToPlace + " at " + x + " " + y);
                    
                    board.choosePiece(id);
                    pieceToPlace = id;
                    gameManager.addToGameSummary(activePlayer.getNicknameToken() + " chose piece " + id);
                }

            } catch (TimeoutException e) {
                String msg = activePlayer.getNicknameToken() + " timed out!";
                activePlayer.deactivate(msg);
                gameManager.addToGameSummary(msg);
                activePlayer.setScore(-1);
                endGame(activePlayer);
                return;
            } catch (Exception e) {
                String msg = activePlayer.getNicknameToken() + ": " + e.getMessage();
                activePlayer.deactivate(msg);
                gameManager.addToGameSummary(msg);
                activePlayer.setScore(-1);
                endGame(activePlayer);
                return;
            }

            viewer.update(board, pieceToPlace, activePlayer.getIndex(), action);
        } catch (Exception e) {
            gameManager.addToGameSummary("Referee error: " + e.getMessage());
            e.printStackTrace();
            activePlayer.setScore(-1);
            endGame(activePlayer);
            return;
        }

        int[] winningLine = board.getWinningLine();
        if (winningLine != null) {
            viewer.drawWinningLine(winningLine);
            activePlayer.setScore(1000);
            Player opponent = gameManager.getPlayer((turn) % 2);
            opponent.setScore(0);
            gameManager.addToGameSummary(activePlayer.getNicknameToken() + " completed a line or square of 4 and wins!");
            gameManager.setFrameDuration(3000); // 1s for the move + 1s for the highlighting
            gameManager.endGame();
        } else if (board.getAvailablePieces().isEmpty() && pieceToPlace == -1) {
            Player p0 = gameManager.getPlayer(0);
            Player p1 = gameManager.getPlayer(1);
            p0.setScore(tieBreakerScore[0]);
            p1.setScore(tieBreakerScore[1]);
            
            gameManager.addToGameSummary("Board is full. Tie-breaker scores applied!");
            gameManager.addToGameSummary(String.format("%s: %d points", p0.getNicknameToken(), tieBreakerScore[0]));
            gameManager.addToGameSummary(String.format("%s: %d points", p1.getNicknameToken(), tieBreakerScore[1]));
            
            if (tieBreakerScore[0] > tieBreakerScore[1]) {
                gameManager.addToGameSummary(p0.getNicknameToken() + " wins the tie-breaker!");
            } else if (tieBreakerScore[1] > tieBreakerScore[0]) {
                gameManager.addToGameSummary(p1.getNicknameToken() + " wins the tie-breaker!");
            } else {
                gameManager.addToGameSummary("It's a perfect tie!");
            }
            
            gameManager.endGame();
        }
    }

    private void endGame(Player loser) {
        Player winner = gameManager.getPlayer((loser.getIndex() + 1) % 2);
        winner.setScore(1);
        gameManager.endGame();
    }

    @Override
    public void onEnd() {
        String[] texts = new String[2];
        Player p0 = gameManager.getPlayer(0);
        Player p1 = gameManager.getPlayer(1);
        
        for (Player p : gameManager.getPlayers()) {
            int i = p.getIndex();
            if (p.getScore() < 0) {
                texts[i] = "Eliminated";
            } else if (p0.getScore() == p1.getScore()) {
                texts[i] = "Draw";
            } else if (p.getScore() > gameManager.getPlayer((i + 1) % 2).getScore()) {
                texts[i] = "Winner";
            } else {
                texts[i] = "Loser";
            }
        }
        viewer.drawEndScreen(texts);
    }
}
