package com.minegusta.demictf;

import com.censoredsoftware.library.util.RandomUtil;
import com.demigodsrpg.demigames.event.*;
import com.demigodsrpg.demigames.game.Game;
import com.demigodsrpg.demigames.game.GameLocation;
import com.demigodsrpg.demigames.game.mixin.ConfinedSpectateMixin;
import com.demigodsrpg.demigames.game.mixin.ErrorTimerMixin;
import com.demigodsrpg.demigames.game.mixin.FakeDeathMixin;
import com.demigodsrpg.demigames.game.mixin.WarmupLobbyMixin;
import com.demigodsrpg.demigames.kit.Kit;
import com.demigodsrpg.demigames.session.Session;
import com.demigodsrpg.demigames.stage.DefaultStage;
import com.demigodsrpg.demigames.stage.StageHandler;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public abstract class CaptureTheFlag implements Game, WarmupLobbyMixin, ErrorTimerMixin, FakeDeathMixin,
        ConfinedSpectateMixin {

    // -- SETTINGS -- //

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

    @Override
    public Location getWarmupSpawn(Session session, Player player) {
        return null;
    }

    @Override
    public int getWarmupSeconds() {
        return 60;
    }

    public abstract int getDefaultFlagLives();

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
        if (event.getGame().isPresent() && event.getGame().get().equals(this)) {
            Optional<Session> opSession = checkPlayer(event.getPlayer());

            if (opSession.isPresent()) {
                assignTeam(opSession.get(), event.getPlayer());
                event.getPlayer().teleport(getWarmupSpawn(opSession.get(), event.getPlayer()));
                event.getPlayer().setGameMode(GameMode.SURVIVAL);

                // TODO Kits
            }
        }
    }

    @Override
    public void onLeave(PlayerQuitMinigameEvent event) {
        if (event.getGame().isPresent() && event.getGame().get().equals(this)) {
            Kit.EMPTY.apply(getBackend(), event.getPlayer(), true);

            Optional<Session> opSession = event.getSession();
            if (opSession.isPresent()) {
                Session session = opSession.get();
                if (session.getProfiles().size() < getMinimumPlayers()) {
                    session.endSession();
                }
            }
        }
    }

    // -- STAGES -- //

    @StageHandler(stage = DefaultStage.SETUP)
    public void roundSetup(Session session) {
        // Make sure the world is present
        if (session.getWorld().isPresent()) {
            // Get the world
            World world = session.getWorld().get();

            // Get blue
            blueSpawn = getLocation("blue.spawn", world.getSpawnLocation());
            blueFlag = getLocation("blue.flag", world.getSpawnLocation());

            // Get red
            redSpawn = getLocation("red.spawn", world.getSpawnLocation());
            redFlag = getLocation("red.flag", world.getSpawnLocation());

            // Get the spectate spawn
            spectatorSpawn = getLocation("spectate", world.getSpawnLocation());

            // Setup spectator data
            session.getData().put("spectators", new ArrayList<String>());

            // Update the stage TODO This isn't the best place to start the warmup
            session.updateStage(DefaultStage.WARMUP, true);
        } else {
            // Update the stage
            session.updateStage(DefaultStage.ERROR, true);
        }
    }

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

    // -- WIN/LOSE/TIE CONDITIONS -- //

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWin(PlayerWinMinigameEvent event) {

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLose(PlayerLoseMinigameEvent event) {

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTie(PlayerTieMinigameEvent event) {

    }

    // -- START & STOP -- //

    @Override
    public void onServerStart() {

    }

    @Override
    public void onServerStop() {
        getBackend().getSessionRegistry().fromGame(this).forEach(com.demigodsrpg.demigames.session.Session::endSession);
    }

    // -- LISTENERS -- //

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Optional<Session> opSession = checkPlayer(event.getPlayer());
        if (Action.LEFT_CLICK_BLOCK == event.getAction()) {
            if (opSession.isPresent()) {
                Player player = event.getPlayer();
                Session session = opSession.get();
                CaptureTheFlagTeam team = getTeam(session, player);
                Location clicked = event.getClickedBlock().getLocation();
                Location flag = getFlag(session, team);
                if(clicked.distance(flag) <= 0.5D) {
                    flagStolen(session, team, player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Optional<Session> opSession = checkPlayer(player);
            if (opSession.isPresent()) {
                Session session = opSession.get();
                CaptureTheFlagTeam team = getTeam(session, player);
                Player other = null;
                if (event.getDamager() instanceof Player) {
                    other = (Player) event.getDamager();
                } else if (event.getDamager() instanceof Projectile &&
                        ((Projectile) event.getDamager()).getShooter() instanceof Player) {
                    other = (Player) ((Projectile) event.getDamager()).getShooter();
                }
                if (team == getTeam(session, other)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // -- PRIVATE HELPER METHODS -- //

    private void flagStolen(Session session, CaptureTheFlagTeam team, Player player) {
        getBackend().broadcastTaggedMessage(session, team + team.name() + "'s flag has been stolen by " +
                player.getDisplayName() + team + "!");
        Bukkit.getScheduler().scheduleSyncRepeatingTask(getBackend(), new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    Location magic = player.getLocation().add(0, 2, 0);
                    magic.getWorld().spigot().playEffect(magic, Effect.TILE_BREAK, team == CaptureTheFlagTeam.RED ?
                                    Material.REDSTONE_BLOCK.getId() : Material.LAPIS_BLOCK.getId(), 0, 0.5F, 0.5F, 0.5F,
                            1 / 10, 10, 40);
                    if (magic.distance(getWarmupSpawn(session, player)) <= 6.66D) {
                        flagCaptured(session, team, player);
                        cancel();
                    }
                } else {
                    getBackend().broadcastTaggedMessage(session, team + player.getDisplayName() + team +
                            " has dropped the " + team.name() + " flag!");
                    cancel();
                }
            }
        }, 10, 10);
    }

    private void flagCaptured(Session session, CaptureTheFlagTeam team, Player player) {
        setFlagLives(session, team, getFlagLives(session, team) - 1);
        // TODO
    }

    private CaptureTheFlagTeam assignTeam(Session session, Player player) {
        int redSize = (int) session.getData().getOrDefault("red.size", 0);
        int blueSize = (int) session.getData().getOrDefault("blue.size", 0);

        boolean red = redSize < blueSize;
        if (redSize == blueSize) {
            red = RandomUtil.randomPercentBool(42.0D); // Blue team tends to lose more often than red
        }
        if (red) {
            session.getData().put("red.size", redSize + 1);
            setTeam(session, player, CaptureTheFlagTeam.RED);
        } else {
            session.getData().put("blue.size", blueSize + 1);
            setTeam(session, player, CaptureTheFlagTeam.BLUE);
        }
        return red ? CaptureTheFlagTeam.RED : CaptureTheFlagTeam.BLUE;
    }

    // -- PRIVATE GETTER/SETTER METHODS -- //

    private Location getFlag(Session session, CaptureTheFlagTeam team) {
        GameLocation flag = team == CaptureTheFlagTeam.RED ? redFlag : blueFlag;
        Optional<Location> flagLoc = flag.toLocation(session.getId());
        if (flagLoc.isPresent()) {
            return flagLoc.get();
        }
        return Bukkit.getWorld(session.getId()).getSpawnLocation();
    }

    private int getFlagLives(Session session, CaptureTheFlagTeam team) {
        if (!session.getData().containsKey("lives." + team.name())) {
            setFlagLives(session, team, getDefaultFlagLives());
        }
        return (int) session.getData().get("lives." + team.name());
    }

    private void setFlagLives(Session session, CaptureTheFlagTeam team, int lives) {
        session.getData().put("lives." + team.name(), lives);
    }

    private CaptureTheFlagTeam getTeam(Session session, Player player) {
        return getTeamData(session).get(player.getName());
    }

    private void setTeam(Session session, Player player, CaptureTheFlagTeam team) {
        getTeamData(session).put(player.getName(), team);
    }

    @SuppressWarnings("unchecked")
    private Map<String, CaptureTheFlagTeam> getTeamData(Session session) {
        if(!session.getData().containsKey("teams")) {
            session.getData().put("teams", new HashMap<String, CaptureTheFlagTeam>());
        }
        return (Map<String, CaptureTheFlagTeam>) session.getData().get("teams");
    }
}
