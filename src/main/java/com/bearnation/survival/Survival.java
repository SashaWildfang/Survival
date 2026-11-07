package com.bearnation.survival;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class Survival extends JavaPlugin implements Listener, TabExecutor {

    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();
    private final Map<UUID, UUID> teleportRequests = new HashMap<>();
    private final Map<UUID, Long> pendingTeleports = new HashMap<>();
    private static Survival instance;

    @Override
    public void onEnable() {
        instance = this;

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("locate").setExecutor(new LocateCommand(this));
        Bukkit.getPluginManager().registerEvents(new GlowManager(), this);
        getCommand("sethome").setExecutor(this);
        getCommand("home").setExecutor(this);
        getCommand("homes").setExecutor(this);
        getCommand("delhome").setExecutor(this);
        getCommand("tpa").setExecutor(this);
        getCommand("tpahere").setExecutor(this);
        getCommand("tpaccept").setExecutor(this);
        getCommand("tpdeny").setExecutor(this);
        new SurvivalScoreboard(this); //

        File dir = getDataFolder();
        if (!dir.exists()) dir.mkdirs();

        new HealthDisplayTask(this).runTaskTimer(this, 0L, 20L);

    }

    public static Survival getInstance() {
        return instance;
    }


    @Override
    public void onDisable() {
        for (UUID uuid : homes.keySet()) saveHomesToFile(uuid);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        loadHomesFromFile(uuid);
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pendingTeleports.containsKey(uuid)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())) return;

        pendingTeleports.remove(uuid);
        event.getPlayer().sendMessage("\u00a7cTeleport cancelled due to movement.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        switch (command.getName().toLowerCase()) {
            case "sethome" -> {
                if (args.length != 1) {
                    player.sendMessage("\u00a7cUsage: /sethome [name]");
                    return true;
                }
                homes.computeIfAbsent(uuid, k -> new HashMap<>()).put(args[0].toLowerCase(), player.getLocation());
                saveHomesToFile(uuid);
                player.sendMessage("\u00a7aHome '" + args[0] + "' set!");
                return true;
            }
            case "delhome" -> {
                if (args.length != 1) {
                    player.sendMessage("\u00a7cUsage: /delhome [name]");
                    return true;
                }
                Map<String, Location> userHomes = homes.get(uuid);
                if (userHomes != null && userHomes.remove(args[0].toLowerCase()) != null) {
                    saveHomesToFile(uuid);
                    player.sendMessage("\u00a7aHome '" + args[0] + "' deleted.");
                } else {
                    player.sendMessage("\u00a7cHome not found: \u00a77" + args[0]);
                }
                return true;
            }
            case "home" -> {
                if (args.length != 1) {
                    sendHomeList(player, uuid);
                    return true;
                }
                Map<String, Location> userHomes = homes.get(uuid);
                String homeName = args[0].toLowerCase();

                if (userHomes == null || !userHomes.containsKey(homeName)) {
                    player.sendMessage("\u00a7cHome not found: \u00a77" + args[0]);
                    sendHomeList(player, uuid);
                    return true;
                }
                player.sendMessage("\u00a7eTeleporting in 3 seconds. Don't move...");
                pendingTeleports.put(uuid, System.currentTimeMillis());

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (pendingTeleports.remove(uuid) != null) {
                        player.teleport(userHomes.get(homeName));
                        player.sendMessage("\u00a7aTeleported to home '" + args[0] + "'.");
                    }
                }, 60L);
                return true;
            }
            case "homes" -> {
                sendHomeList(player, uuid);
                return true;
            }
            case "tpa", "tpahere", "tpaccept", "tpdeny" -> {
                return handleTPCommand(command.getName().toLowerCase(), player, args);
            }
        }
        return false;
    }

    private boolean handleTPCommand(String name, Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        if (name.equals("tpaccept")) {
            UUID requester = teleportRequests.remove(uuid);
            if (requester == null) {
                player.sendMessage("\u00a7cYou have no teleport requests.");
                return true;
            }
            Player senderPlayer = Bukkit.getPlayer(requester);
            if (senderPlayer == null || !senderPlayer.isOnline()) {
                player.sendMessage("\u00a7cRequester is no longer online.");
                return true;
            }
            senderPlayer.sendMessage("\u00a7eTeleporting in 3 seconds. Don't move...");
            pendingTeleports.put(senderPlayer.getUniqueId(), System.currentTimeMillis());
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (pendingTeleports.remove(senderPlayer.getUniqueId()) != null) {
                    senderPlayer.teleport(player.getLocation());
                    senderPlayer.sendMessage("\u00a7aTeleported to " + player.getName() + "!");
                }
            }, 60L);
            player.sendMessage("\u00a7aAccepted teleport request.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("\u00a7cUsage: /" + name + " [player]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("\u00a7cPlayer not found.");
            return true;
        }

        if (name.equals("tpa")) {
            teleportRequests.put(target.getUniqueId(), uuid);
            player.sendMessage("\u00a7aTeleport request sent to " + target.getName() + ".");
            target.sendMessage("\u00a7e" + player.getName() + " wants to teleport to you. Type \u00a7a/tpaccept \u00a7eor \u00a7c/tpdeny.");
        } else if (name.equals("tpahere")) {
            teleportRequests.put(target.getUniqueId(), uuid);
            player.sendMessage("\u00a7aTeleport-here request sent to " + target.getName() + ".");
            target.sendMessage("\u00a7e" + player.getName() + " wants you to teleport to them. Type \u00a7a/tpaccept \u00a7eor \u00a7c/tpdeny.");
        } else if (name.equals("tpdeny")) {
            UUID denied = teleportRequests.remove(uuid);
            if (denied != null) {
                Player deniedPlayer = Bukkit.getPlayer(denied);
                if (deniedPlayer != null) deniedPlayer.sendMessage("\u00a7cYour teleport request was denied.");
            }
            player.sendMessage("\u00a7aTeleport request denied.");
        }
        return true;
    }

    private void sendHomeList(Player player, UUID uuid) {
        Map<String, Location> userHomes = homes.get(uuid);
        if (userHomes == null || userHomes.isEmpty()) {
            player.sendMessage("\u00a77You have no homes set.");
        } else {
            player.sendMessage("\u00a7eYour Homes:");
            for (String name : userHomes.keySet()) {
                player.sendMessage("\u00a7f - \u00a7a" + name);
            }
        }
    }

    private void saveHomesToFile(UUID uuid) {
        File file = new File(getDataFolder(), uuid + "_homes.yml");
        FileConfiguration config = new YamlConfiguration();
        Map<String, Location> userHomes = homes.get(uuid);
        if (userHomes != null) {
            for (Map.Entry<String, Location> entry : userHomes.entrySet()) {
                Location loc = entry.getValue();
                String path = "homes." + entry.getKey();
                config.set(path + ".world", loc.getWorld().getName());
                config.set(path + ".x", loc.getX());
                config.set(path + ".y", loc.getY());
                config.set(path + ".z", loc.getZ());
                config.set(path + ".yaw", loc.getYaw());
                config.set(path + ".pitch", loc.getPitch());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHomesFromFile(UUID uuid) {
        File file = new File(getDataFolder(), uuid + "_homes.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, Location> userHomes = new HashMap<>();

        if (config.contains("homes")) {
            for (String key : config.getConfigurationSection("homes").getKeys(false)) {
                String path = "homes." + key;
                World world = Bukkit.getWorld(config.getString(path + ".world"));
                if (world == null) continue;
                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                float yaw = (float) config.getDouble(path + ".yaw");
                float pitch = (float) config.getDouble(path + ".pitch");
                userHomes.put(key.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
            }
        }
        homes.put(uuid, userHomes);
    }
}