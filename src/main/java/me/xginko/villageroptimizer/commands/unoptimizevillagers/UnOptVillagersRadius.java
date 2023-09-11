package me.xginko.villageroptimizer.commands.unoptimizevillagers;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.models.WrappedVillager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

public class UnOptVillagersRadius implements VillagerOptimizerCommand {

    /*
     * TODO: Radius limit, Cooldown, Compatibility with other types
     *
     * */

    @Override
    public String label() {
        return "unoptimizevillagers";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender.hasPermission(Permissions.Commands.UNOPTIMIZE_RADIUS.get())) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be executed by a player.")
                        .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                return true;
            }

            if (args.length != 1) {
                VillagerOptimizer.getLang(player.locale()).command_specify_radius.forEach(player::sendMessage);
                return true;
            }

            try {
                int specifiedRadius = Integer.parseInt(args[0]) / 2;

                VillagerManager villagerManager = VillagerOptimizer.getVillagerManager();
                int successCount = 0;

                for (Entity entity : player.getNearbyEntities(specifiedRadius, specifiedRadius, specifiedRadius)) {
                    if (!entity.getType().equals(EntityType.VILLAGER)) continue;
                    Villager villager = (Villager) entity;
                    Villager.Profession profession = villager.getProfession();
                    if (profession.equals(Villager.Profession.NITWIT) || profession.equals(Villager.Profession.NONE)) continue;

                    WrappedVillager wVillager = villagerManager.getOrAdd(villager);

                    if (wVillager.isOptimized()) {
                        wVillager.setOptimization(OptimizationType.OFF);
                        successCount++;
                    }
                }

                final String successfullyUnoptimized = Integer.toString(successCount);
                final String radius = Integer.toString(specifiedRadius);
                VillagerOptimizer.getLang(player.locale()).command_unoptimize_success.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%amount%").replacement(successfullyUnoptimized).build())
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%radius%").replacement(radius).build())
                ));
            } catch (NumberFormatException e) {
                VillagerOptimizer.getLang(player.locale()).command_radius_invalid.forEach(player::sendMessage);
            }
        } else {
            sender.sendMessage(VillagerOptimizer.getLang(sender).no_permission);
        }
        return true;
    }
}
