package me.xginko.villageroptimizer.modules;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.utils.LogUtils;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

public class VillagerChunkLimit implements VillagerOptimizerModule, Listener {

    private final VillagerOptimizer plugin;
    private final VillagerManager villagerManager;
    private ScheduledTask scheduledTask;
    private final List<Villager.Profession> removalPriority = new ArrayList<>(16);
    private final int maxVillagersPerChunk;
    private final boolean logIsEnabled;
    private final long checkPeriod;

    protected VillagerChunkLimit() {
        shouldEnable();
        this.plugin = VillagerOptimizer.getInstance();
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        Config config = VillagerOptimizer.getConfiguration();
        this.maxVillagersPerChunk = config.getInt("villager-chunk-limit.max-villagers-per-chunk", 25);
        this.logIsEnabled = config.getBoolean("villager-chunk-limit.log-removals", false);
        this.checkPeriod = config.getInt("villager-chunk-limit.check-period-in-ticks", 600, "check all chunks every x ticks.");
        config.getList("villager-chunk-limit.removal-priority", List.of(
                "NONE", "NITWIT", "SHEPHERD", "FISHERMAN", "BUTCHER", "CARTOGRAPHER", "LEATHERWORKER",
                "FLETCHER", "MASON", "FARMER", "ARMORER", "TOOLSMITH", "WEAPONSMITH", "CLERIC", "LIBRARIAN"
        ),
                "Professions that are in the top of the list are going to be scheduled for removal first."

        ).forEach(configuredProfession -> {
            try {
                Villager.Profession profession = Villager.Profession.valueOf(configuredProfession);
                this.removalPriority.add(profession);
            } catch (IllegalArgumentException e) {
                LogUtils.moduleLog(Level.WARNING, "villager-chunk-limit", "Villager profession '"+configuredProfession+"' not recognized. Make sure you're using the correct profession enums.");
            }
        });
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.scheduledTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> run(), checkPeriod, checkPeriod);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("villager-chunk-limit.enable", false);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (scheduledTask != null) scheduledTask.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity spawned = event.getEntity();
        if (spawned.getType().equals(EntityType.VILLAGER)) {
            checkVillagersInChunk(spawned.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (clicked.getType().equals(EntityType.VILLAGER)) {
            checkVillagersInChunk(clicked.getChunk());
        }
    }

    private void run() {
        plugin.getServer().getWorlds().forEach(world -> {
            for (Chunk chunk : world.getLoadedChunks()) {
                plugin.getServer().getRegionScheduler().run(plugin, world, chunk.getX(), chunk.getZ(), task -> checkVillagersInChunk(chunk));
            }
        });
    }

    private void checkVillagersInChunk(Chunk chunk) {
        // Create a list with all villagers in that chunk
        List<Villager> villagers_in_chunk = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType().equals(EntityType.VILLAGER)) {
                villagers_in_chunk.add((Villager) entity);
            }
        }

        // Check if there are more villagers in that chunk than allowed
        int amount_over_the_limit = villagers_in_chunk.size() - maxVillagersPerChunk;
        if (amount_over_the_limit <= 0) return;

        // Sort villager list by profession priority
        villagers_in_chunk.sort(Comparator.comparingInt(this::getProfessionPriority));

        // Remove prioritized villagers that are too many
        for (int i = 0; i < amount_over_the_limit; i++) {
            Villager villager = villagers_in_chunk.get(i);
            villager.remove();
            if (logIsEnabled) LogUtils.moduleLog(Level.INFO, "villager-chunk-limit",
                    "Removed villager of profession type '"+villager.getProfession()+"' at "+villager.getLocation());
        }
    }

    private int getProfessionPriority(Villager villager) {
        final Villager.Profession profession = villager.getProfession();
        return removalPriority.contains(profession) && !villagerManager.getOrAdd(villager).isOptimized() ? removalPriority.indexOf(profession) : Integer.MAX_VALUE;
    }
}