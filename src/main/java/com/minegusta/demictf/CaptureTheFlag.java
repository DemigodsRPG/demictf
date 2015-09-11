package com.minegusta.demictf;

import com.censoredsoftware.library.util.RandomUtil;
import com.demigodsrpg.demigames.event.*;
import com.demigodsrpg.demigames.game.Game;
import com.demigodsrpg.demigames.game.GameLocation;
import com.demigodsrpg.demigames.game.mixin.ErrorTimerMixin;
import com.demigodsrpg.demigames.game.mixin.FakeDeathMixin;
import com.demigodsrpg.demigames.game.mixin.WarmupLobbyMixin;
import com.demigodsrpg.demigames.kit.Kit;
import com.demigodsrpg.demigames.session.Session;
import com.demigodsrpg.demigames.stage.DefaultStage;
import com.demigodsrpg.demigames.stage.StageHandler;
import com.demigodsrpg.demigames.unlockable.UnlockableKit;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;

import java.util.*;

public abstract class CaptureTheFlag implements Game, WarmupLobbyMixin, ErrorTimerMixin, FakeDeathMixin {

    // -- KITS -- //

    // Free
    final UnlockableKit SABER = createKit("ctf_saber", new MaterialData(Material.IRON_SWORD),
            "Fight with swords.");
    final UnlockableKit ARCHER = createKit("ctf_archer", new MaterialData(Material.BOW),
            "Battle with a bow and arrows.");
    final UnlockableKit CASTER = createKit("ctf_caster", new MaterialData(Material.POTION),
            "Use magic to overpower your enemies.");

    // Premium
    final UnlockableKit LANCER = createKit("ctf_lancer", 100, new MaterialData(Material.DIAMOND_SPADE),
            "Fight with a lance and be the ultimate bro.");
    final UnlockableKit RIDER = createKit("ctf_rider", 100, new MaterialData(Material.SADDLE),
            "Use your power over mounts to your advantage.");
    final UnlockableKit BERSERKER = createKit("ctf_berserker", 100, new MaterialData(Material.FIREBALL),
            "Unleash your uncontrollable rage.");
    final UnlockableKit ASSASSIN = createKit("ctf_assassin", 100, new MaterialData(Material.GHAST_TEAR),
            "Skillfully take down enemies.");

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
        return Arrays.asList(SABER.getName(), ARCHER.getName(), CASTER.getName());
    }

    /* @Override
    public Location getSpectatorSpawn(Session session) {
        Optional<Location> spawn = spectatorSpawn.toLocation(session.getId());
        if (spawn.isPresent()) {
            return spawn.get();
        }
        return Bukkit.getWorld(session.getId()).getSpawnLocation();
    }*/

    @Override
    public Location getWarmupSpawn(Session session, Player player) {
        GameLocation spawn = getTeam(session, player) == CaptureTheFlagTeam.RED ? redSpawn : blueSpawn;
        Optional<Location> spawnLoc = spawn.toLocation(session.getId());
        if (spawnLoc.isPresent()) {
            return spawnLoc.get();
        }
        return Bukkit.getWorld(session.getId()).getSpawnLocation();
    }

    @Override
    public int getWarmupSeconds() {
        return 30;
    }

    public abstract int getDefaultFlagLives();

    // -- LOCATIONS -- //

    // Spectator
    //GameLocation spectatorSpawn;

    // Blue
    GameLocation blueSpawn;
    GameLocation blueFlag;

    // Red
    GameLocation redSpawn;
    GameLocation redFlag;

    // -- TASKS -- //

    // -- JOIN/LEAVE -- //

    @Override
    @EventHandler(priority = EventPriority.LOWEST)
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
    @EventHandler(priority = EventPriority.LOWEST)
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

            // Set the flags to correct colors
            if (redFlag.toLocation(session.getId()).isPresent()) {
                redFlag.toLocation(session.getId()).get().getBlock().setType(Material.REDSTONE_BLOCK);
            }
            if (blueFlag.toLocation(session.getId()).isPresent()) {
                blueFlag.toLocation(session.getId()).get().getBlock().setType(Material.LAPIS_BLOCK);
            }

            // Get the spectate spawn
            //spectatorSpawn = getLocation("spectate", world.getSpawnLocation());

            // Setup spectator data
            //session.getData().put("spectators", new ArrayList<String>());

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
        session.getData().put("task." + team.name(), Bukkit.getScheduler().
                scheduleSyncRepeatingTask(getBackend(), () -> {
                    if (player.isOnline()) {
                        Location magic = player.getLocation().add(0, 2, 0);
                        magic.getWorld().spigot().playEffect(magic, Effect.TILE_BREAK, team == CaptureTheFlagTeam.RED ?
                                        Material.REDSTONE_BLOCK.getId() : Material.LAPIS_BLOCK.getId(), 0, 0.5F, 0.5F, 0.5F,
                                1 / 10, 10, 40);
                        if (magic.distance(getWarmupSpawn(session, player)) <= 6.66D) {
                            flagCaptured(session, team, player);
                            Bukkit.getScheduler().cancelTask((int) session.getData().get("task." + team.name()));
                        }
                    } else {
                        getBackend().broadcastTaggedMessage(session, team + player.getDisplayName() + team +
                                " has dropped the " + team.name() + " flag!");
                        Bukkit.getScheduler().cancelTask((int) session.getData().get("task." + team.name()));
                    }
                    if (session.isDone()) {
                        Bukkit.getScheduler().cancelTask((int) session.getData().get("task." + team.name()));
                    }
                }, 10, 10));
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

    private UnlockableKit createKit(String name, MaterialData data, String... lore) {
        return new UnlockableKit(getBackend(), name, 0, lore, false, false, data.getItemType(), data.getData());
    }

    private UnlockableKit createKit(String name, int cost, MaterialData data, String... lore) {
        return new UnlockableKit(getBackend(), name, cost, lore, false, true, data.getItemType(), data.getData());
    }
}
