package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class RenameOptimizedVillagers implements VillagerOptimizerModule, Listener {

    private final VillagerOptimizer plugin;
    private final Component optimized_name;
    private final boolean overwrite_previous_name;

    protected RenameOptimizedVillagers() {
        this.plugin = VillagerOptimizer.getInstance();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("general.rename-villagers.enable", """
                Will change a villager's name to the name configured below when they are optimized.\s
                These names will be removed when unoptimized again if they were not changed in the meantime.
                """);
        this.optimized_name = MiniMessage.miniMessage().deserialize(config.getString("general.rename-villagers.optimized-name", "<green>Optimized",
                "The name that will be used to mark optimized villagers. Uses MiniMessage format."));
        this.overwrite_previous_name = config.getBoolean("general.rename-villagers.overwrite-existing-name", false,
                "If set to true, will rename even if the villager has already been named.");
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("general.rename-villagers.enable", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onOptimize(VillagerOptimizeEvent event) {
        WrappedVillager wVillager = event.getWrappedVillager();
        wVillager.villager().getScheduler().runDelayed(plugin, rename -> {
            wVillager.rename(optimized_name, overwrite_previous_name);
        }, null, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onUnOptimize(VillagerUnoptimizeEvent event) {
        WrappedVillager wVillager = event.getWrappedVillager();
        if (wVillager.getOptimizationType().equals(OptimizationType.NAMETAG)) return;

        wVillager.villager().getScheduler().runDelayed(plugin, rename -> {
            wVillager.rename(null, overwrite_previous_name);
        }, null, 10L);
    }
}
