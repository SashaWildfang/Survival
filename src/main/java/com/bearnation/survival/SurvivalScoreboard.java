package com.bearnation.survival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SurvivalScoreboard implements Listener {

    private final Map<UUID, Long> sessionStart = new HashMap<>();

    public SurvivalScoreboard(Survival plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!sessionStart.containsKey(player.getUniqueId())) {
                        sessionStart.put(player.getUniqueId(), System.currentTimeMillis());
                    }
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void updateScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("survival", "dummy", ChatColor.GREEN + "" + ChatColor.BOLD + "Survival " + ChatColor.GRAY + "(" + Bukkit.getOnlinePlayers().size() + ")");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;
        Location loc = player.getLocation();

        long sessionSeconds = (System.currentTimeMillis() - sessionStart.getOrDefault(player.getUniqueId(), System.currentTimeMillis())) / 1000;
        long lifetimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long lifetimeSeconds = lifetimeTicks / 20 * 60; // PLAY_ONE_MINUTE is in ticks/20

        obj.getScore(" ").setScore(line--);
        obj.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "GENERAL").setScore(line--);

        Biome biome = loc.getBlock().getBiome();
        @SuppressWarnings("removal")
        String biomeName = biome.toString().toLowerCase().replace("_", " ");
        biomeName = Character.toUpperCase(biomeName.charAt(0)) + biomeName.substring(1);

        obj.getScore(ChatColor.GRAY + "Biome: " + ChatColor.AQUA + biomeName).setScore(line--);
        obj.getScore(ChatColor.GRAY + "Coords: " + ChatColor.GREEN + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()).setScore(line--);

        obj.getScore("  ").setScore(line--);
        obj.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "PLAYTIME").setScore(line--);
        obj.getScore(ChatColor.GRAY + "Session: " + ChatColor.GREEN + formatTime(sessionSeconds)).setScore(line--);
        obj.getScore(ChatColor.GRAY + "Lifetime: " + ChatColor.GREEN + formatTime(lifetimeSeconds)).setScore(line--);

        obj.getScore("   ").setScore(line--);
        obj.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "STATS").setScore(line--);
        obj.getScore(ChatColor.GRAY + "Kills: " + ChatColor.RED + player.getStatistic(Statistic.PLAYER_KILLS)).setScore(line--);
        obj.getScore(ChatColor.GRAY + "Deaths: " + ChatColor.RED + player.getStatistic(Statistic.DEATHS)).setScore(line--);

        obj.getScore("    ").setScore(line--);
        obj.getScore(ChatColor.BLUE + "play.mcbearnation.com").setScore(line--);

        player.setScoreboard(board); // Update only this player's scoreboard
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return h + "h " + m + "m";
    }
}
