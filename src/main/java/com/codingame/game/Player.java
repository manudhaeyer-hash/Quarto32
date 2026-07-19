package com.codingame.game;

import com.codingame.gameengine.core.AbstractMultiplayerPlayer;

public class Player extends AbstractMultiplayerPlayer {

    @Override
    public int getExpectedOutputLines() {
        // Nombre de lignes attendues du joueur à chaque tour.
        return 1;
    }
}
