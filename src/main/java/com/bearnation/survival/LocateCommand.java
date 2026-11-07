package com.bearnation.survival;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class LocateCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public LocateCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    record Node(Location loc, double cost, double heuristic, Node parent) {
        double total() {
            return cost + heuristic;
        }
    }

    private List<Location> findPath(Location start, Location goal) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::total));
        Set<String> visited = new HashSet<>();
        open.add(new Node(start, 0, heuristic(start, goal), null));

        while (!open.isEmpty()) {
            Node current = open.poll();
            Location loc = current.loc;
            String key = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            if (visited.contains(key)) continue;
            visited.add(key);

            if (loc.getBlockX() == goal.getBlockX() && loc.getBlockZ() == goal.getBlockZ()) {
                List<Location> path = new ArrayList<>();
                while (current != null) {
                    path.add(current.loc.clone().add(0.5, 0, 0.5)); // center of block
                    current = current.parent;
                }
                Collections.reverse(path);
                return path;
            }

            for (Vector dir : List.of(
                    new Vector(1, 0, 0), new Vector(-1, 0, 0),
                    new Vector(0, 0, 1), new Vector(0, 0, -1)
            )) {
                Location neighbor = loc.clone().add(dir);
                neighbor.setY(loc.getY()); // stay at current Y level to avoid jumping
                String nKey = neighbor.getBlockX() + "," + neighbor.getBlockY() + "," + neighbor.getBlockZ();
                if (visited.contains(nKey)) continue;
                double cost = current.cost + loc.distance(neighbor);
                double h = heuristic(neighbor, goal);
                open.add(new Node(neighbor, cost, h, current));
            }
        }

        return List.of(); // No path
    }

    private double heuristic(Location a, Location b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /locate [player]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cPlayer not found.");
            return true;
        }

        if (activeTasks.containsKey(player.getUniqueId())) {
            activeTasks.get(player.getUniqueId()).cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            List<Location> path = findPath(player.getLocation(), target.getLocation());
            Location lastTargetLoc = target.getLocation().clone();
            int pulse = 0;

            @Override
            public void run() {
                if (!target.isOnline() || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Recalculate if the target moved significantly
                if (target.getLocation().distanceSquared(lastTargetLoc) > 4) {
                    path = findPath(player.getLocation(), target.getLocation());
                    lastTargetLoc = target.getLocation().clone();
                    pulse = 0;
                }

                if (pulse >= 5) {
                    cancel();
                    return;
                }

                // Gold color (RGB: 255, 215, 0)
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5F);

                for (int i = 0; i < path.size() - 1; i++) {
                    Location from = path.get(i).clone().add(0, 1.2, 0); // lift above ground
                    Location to = path.get(i + 1).clone().add(0, 1.2, 0);
                    Vector step = to.toVector().subtract(from.toVector()).normalize().multiply(0.3);

                    for (double d = 0; d < from.distance(to); d += 0.3) {
                        Location point = from.clone().add(step.clone().multiply(d));
                        player.spawnParticle(Particle.DUST, point, 2, 0, 0, 0, 0, dust, true);
                    }
                }

                pulse++;
            }

            @Override
            public synchronized void cancel() {
                super.cancel();
                activeTasks.remove(player.getUniqueId());
            }

        };

        task.runTaskTimer(plugin, 0L, 20L); // run every second
        activeTasks.put(player.getUniqueId(), task);

        player.sendMessage("§aPath to §e" + target.getName() + " §ahas been created.");
        return true;
    }
}
