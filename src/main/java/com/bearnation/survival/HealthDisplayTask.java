package com.bearnation.survival;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HealthDisplayTask extends BukkitRunnable implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final long FADE_OUT_DELAY_MS = 5000; // 5 seconds
    private final double VISIBILITY_RADIUS_SQ = 30 * 30;

    public HealthDisplayTask(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Player) continue;

                boolean isVisibleToAnyPlayer = false;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(world)) continue;
                    if (entity.getLocation().distanceSquared(player.getLocation()) > VISIBILITY_RADIUS_SQ) continue;
                    if (!player.hasLineOfSight(entity)) continue;

                    isVisibleToAnyPlayer = true;
                    break;
                }

                if (isVisibleToAnyPlayer) {
                    updateMobHealthTag(entity, now);
                } else {
                    entity.customName(null);
                    entity.setCustomNameVisible(false);
                }
            }
        }

        // Setup health below player names
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = scoreboard.getObjective("health");

        if (objective == null) {
            objective = scoreboard.registerNewObjective(
                    "health",
                    org.bukkit.scoreboard.Criteria.HEALTH,
                    Component.text("❤", NamedTextColor.RED)
            );
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    private void updateMobHealthTag(LivingEntity entity, long now) {
        double health = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        UUID id = entity.getUniqueId();

        // Fade out if full health and not recently damaged
        if (health >= maxHealth) {
            Long lastHit = lastDamageTime.get(id);
            if (lastHit == null || now - lastHit > FADE_OUT_DELAY_MS) {
                entity.customName(null);
                entity.setCustomNameVisible(false);
                return;
            }
        }

        double ratio = health / maxHealth;
        NamedTextColor color = NamedTextColor.GREEN;
        if (ratio < 0.7) color = NamedTextColor.YELLOW;
        if (ratio < 0.3) color = NamedTextColor.RED;

        Component tag = Component.text(String.format("%.0f", health), color)
                .append(Component.text("/", NamedTextColor.DARK_GRAY))
                .append(Component.text(String.format("%.0f", maxHealth), NamedTextColor.WHITE))
                .append(Component.space())
                .append(Component.text("❤", NamedTextColor.RED));

        entity.customName(tag);
        entity.setCustomNameVisible(true);
    }

    @EventHandler
    public void onMobDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && !(entity instanceof Player)) {
            lastDamageTime.put(entity.getUniqueId(), System.currentTimeMillis());
        }
    }
}
