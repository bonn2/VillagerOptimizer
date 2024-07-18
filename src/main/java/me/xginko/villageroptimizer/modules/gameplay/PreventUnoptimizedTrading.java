package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;

public class PreventUnoptimizedTrading extends VillagerOptimizerModule implements Listener {

    private final boolean first_trade_exempt;
    private final boolean notify_player;

    public PreventUnoptimizedTrading() {
        super("gameplay.prevent-trading-with-unoptimized");
        config.master().addComment(configPath + ".enable",
                "Will prevent players from selecting and using trades of unoptimized villagers.\n" +
                "Use this if you have a lot of villagers and therefore want to force your players to optimize them.\n" +
                "Inventories can still be opened so players can move villagers around.");
        this.first_trade_exempt = config.getBoolean(configPath + ".first-trade-exempt", true,
                "The first trade made with a villager is exempt from this rule. This helps with villager rolling.");
        this.notify_player = config.getBoolean(configPath + ".notify-player", true,
                "Sends players a message when they try to trade with an unoptimized villager.");
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
        return config.getBoolean(configPath + ".enable", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onTradeOpen(TradeSelectEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getWhoClicked().hasPermission(Permissions.Bypass.TRADE_PREVENTION.get())) return;
        if (!(event.getInventory().getHolder() instanceof Villager)) return;
        if (wrapperCache.get((Villager) event.getInventory().getHolder()).isOptimized()) return;
        if (first_trade_exempt && ((Villager) event.getInventory().getHolder()).getVillagerExperience() == 0
        && ((Villager) event.getInventory().getHolder()).getVillagerLevel() == 1) return;

        event.setCancelled(true);

        if (notify_player) {
            Player player = (Player) event.getWhoClicked();
            VillagerOptimizer.getLang(player.locale()).optimize_for_trading.forEach(line -> KyoriUtil.sendMessage(player, line));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getWhoClicked().hasPermission(Permissions.Bypass.TRADE_PREVENTION.get())) return;
        if (!(event.getInventory().getHolder() instanceof Villager)) return;
        if (wrapperCache.get((Villager) event.getInventory().getHolder()).isOptimized()) return;
        if (first_trade_exempt && ((Villager) event.getInventory().getHolder()).getVillagerExperience() == 0
        && ((Villager) event.getInventory().getHolder()).getVillagerLevel() == 1) return;

        event.setCancelled(true);

        if (notify_player) {
            Player player = (Player) event.getWhoClicked();
            VillagerOptimizer.getLang(player.locale()).optimize_for_trading.forEach(line -> KyoriUtil.sendMessage(player, line));
        }
    }
}
