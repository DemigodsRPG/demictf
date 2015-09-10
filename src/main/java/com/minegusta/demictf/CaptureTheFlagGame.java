package com.minegusta.demictf;

import com.demigodsrpg.demigames.event.PlayerJoinMinigameEvent;
import com.demigodsrpg.demigames.event.PlayerQuitMinigameEvent;
import com.demigodsrpg.demigames.game.Game;
import com.demigodsrpg.demigames.game.GameLocation;
import com.demigodsrpg.demigames.game.mixin.ConfinedSpectateMixin;
import com.demigodsrpg.demigames.game.mixin.ErrorTimerMixin;
import com.demigodsrpg.demigames.game.mixin.FakeDeathMixin;
import com.demigodsrpg.demigames.game.mixin.WarmupLobbyMixin;
import com.demigodsrpg.demigames.session.Session;
import com.demigodsrpg.demigames.stage.DefaultStage;
import com.demigodsrpg.demigames.stage.StageHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class CaptureTheFlagGame implements Game, WarmupLobbyMixin, ErrorTimerMixin, FakeDeathMixin,
        ConfinedSpectateMixin {

    // -- CONST -- //

    static int FLAG_LIVES = 3;

    // -- SETTINGS -- //

    @Override
    public String getName() {
        return "Capture The Flag";
    }

    @Override
    public String getDirectory() {
        return "ctf";
    }

    @Override
    public GameMode getDefaultGamemode() {
        return GameMode.ADVENTURE;
    }

    @Override
    public boolean canPlace() {
        return false;
    }

    @Override
    public boolean canBreak() {
        return false;
    }

    @Override
    public boolean canDrop() {
        return false;
    }

    @Override
    public int getMinimumPlayers() {
        return 2;
    }

    @Override
    public int getMaximumPlayers() {
        return 20;
    }

    @Override
    public int getNumberOfTeams() {
        return 2;
    }

    @Override
    public List<String> getDefaultUnlockables() {
        return new ArrayList<>(); // No default unlockables for this game
    }

    @Override
    public Location getSpectatorSpawn(Session session) {
        Optional<Location> spawn = spectatorSpawn.toLocation(session.getId());
        if (spawn.isPresent()) {
            return spawn.get();
        }
        return Bukkit.getWorld(session.getId()).getSpawnLocation();
    }

    // -- LOCATIONS -- //

    // Spectator
    GameLocation spectatorSpawn;

    // Blue
    GameLocation blueSpawn;
    GameLocation blueFlag;

    // Red
    GameLocation redSpawn;
    GameLocation redFlag;

    // -- TASKS -- //

    // -- JOIN/LEAVE -- //

    @Override
    public void onJoin(PlayerJoinMinigameEvent event) {

    }

    @Override
    public void onLeave(PlayerQuitMinigameEvent event) {

    }

    @Override
    public Location getWarmupSpawn(Session session, Player player) {
        return null;
    }

    // -- STAGES -- //

    @StageHandler(stage = DefaultStage.BEGIN)
    public void roundBegin(Session session) {

        // Update the stage
        session.updateStage(DefaultStage.PLAY, true);
    }

    @StageHandler(stage = DefaultStage.PLAY)
    public void roundPlay(Session session) {
        // TODO
    }

    @StageHandler(stage = DefaultStage.END)
    public void roundEnd(Session session) {

        // Update the stage
        session.updateStage(DefaultStage.COOLDOWN, true);
    }

    @StageHandler(stage = DefaultStage.COOLDOWN)
    public void roundCooldown(Session session) {

        // Update the stage
        if (session.getCurrentRound() == getTotalRounds()) {
            session.endSession();
        } else {
            session.updateStage(DefaultStage.RESET, true);
        }
    }

    @StageHandler(stage = DefaultStage.RESET)
    public void roundReset(Session session) {

        // Update the stage
        session.updateStage(DefaultStage.SETUP, true);
    }

    // -- LISTENERS -- //

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Optional<Session> opSession = checkPlayer(event.getPlayer());
        if (Action.LEFT_CLICK_BLOCK == event.getAction()) {
            if (opSession.isPresent()) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                Session session = opSession.get();
                CaptureTeam team = getTeam(session, player);
                Location clicked = event.getClickedBlock().getLocation();
                Location flag = getFlag(session, team);
                if(clicked.distance(flag) <= 0.5D) {
                    flagCaptured(session, team, player);
                }
            }
        }
    }

    // -- PRIVATE HELPER METHODS -- //

    private void flagCaptured(Session session, CaptureTeam team, Player player) {

    }

    // -- PRIVATE GETTER/SETTER METHODS -- //

    private Location getFlag(Session session, CaptureTeam team) {
        GameLocation flag = team == CaptureTeam.RED ? redFlag : blueFlag;
        Optional<Location> flagLoc = flag.toLocation(session.getId());
        if (flagLoc.isPresent()) {
            return flagLoc.get();
        }
        return Bukkit.getWorld(session.getId()).getSpawnLocation();
    }

    private CaptureTeam getTeam(Session session, Player player) {
        return getTeamData(session).get(player.getName());
    }

    private void setTeam(Session session, Player player, CaptureTeam team) {
        getTeamData(session).put(player.getName(), team);
    }

    @SuppressWarnings("unchecked")
    private Map<String, CaptureTeam> getTeamData(Session session) {
        if(!session.getData().containsKey("teams")) {
            session.getData().put("teams", new HashMap<String, CaptureTeam>());
        }
        return (Map<String, CaptureTeam>) session.getData().get("teams");
    }
}
