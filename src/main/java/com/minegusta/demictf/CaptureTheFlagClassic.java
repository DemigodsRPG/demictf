package com.minegusta.demictf;

import com.demigodsrpg.demigames.game.Minigame;

@Minigame
public class CaptureTheFlagClassic extends CaptureTheFlag {
    @Override
    public String getName() {
        return "Capture The Flag - Classic";
    }

    @Override
    public String getDirectory() {
        return "ctf_classic";
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
