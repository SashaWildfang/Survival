package com.bearnation.survival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class GlowManager implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Use main scoreboard to apply glow so it's persistent and not overridden
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        if (player.getName().equalsIgnoreCase("Welshbolt")) {
            Team greenTeam = board.getTeam("green_glow");
            if (greenTeam == null) {
                greenTeam = board.registerNewTeam("green_glow");
                greenTeam.setColor(ChatColor.GREEN);
                greenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                greenTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            }
            greenTeam.addEntry(player.getName());
            player.setGlowing(true);
        } else if (player.getName().equalsIgnoreCase("PlapHuskyCookie") || player.getName().equalsIgnoreCase("DetectivePlapper")) {
            Team orangeTeam = board.getTeam("orange_glow");
            if (orangeTeam == null) {
                orangeTeam = board.registerNewTeam("orange_glow");
                orangeTeam.setColor(ChatColor.GOLD);
                orangeTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                orangeTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            }
            orangeTeam.addEntry(player.getName());
            player.setGlowing(true);
        }
    }
}
