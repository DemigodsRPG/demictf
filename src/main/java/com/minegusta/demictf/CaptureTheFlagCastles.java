package com.minegusta.demictf;

import com.demigodsrpg.demigames.game.Minigame;

@Minigame
public class CaptureTheFlagCastles extends CaptureTheFlag {
    @Override
    public String getName() {
        return "Capture The Flag - Castles";
    }

    @Override
    public String getDirectory() {
        return "ctf_castles";
    }

    @Override
    public int getMaximumPlayers() {
        return 20;
    }

    @Override
    public int getDefaultFlagLives() {
        return 3;
    }
}
