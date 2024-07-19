package me.xginko.villageroptimizer.commands.optimizevillagers;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrapperCache;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import me.xginko.villageroptimizer.utils.Util;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
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
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OptVillagerFacing extends VillagerOptimizerCommand {

    private final long cooldown;
    private final int max_distance;

    public OptVillagerFacing() {
        super("optimizevillager");
        Config config = VillagerOptimizer.config();
        this.cooldown = config.getInt("optimization-methods.commands.optimizevillager.cooldown-seconds", 600,
                "Cooldown in seconds until a villager can be optimized again using the command.\n" +
                        "Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.") * 1000L;
        this.max_distance = config.getInt("optimization-methods.commands.optimizevillager.max-distance", 5,
                "The number of blocks away a targeted villager can be.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!sender.hasPermission(Permissions.Commands.OPTIMIZE_FACING.get())) {
            KyoriUtil.sendMessage(sender, VillagerOptimizer.getLang(sender).no_permission);
            return true;
        }

        if (!(sender instanceof Player)) {
            KyoriUtil.sendMessage(sender, Component.text("This command can only be executed by a player.")
                    .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            return true;
        }

        Player player = (Player) sender;
        final boolean player_has_cooldown_bypass = player.hasPermission(Permissions.Bypass.COMMAND_COOLDOWN.get());

        WrapperCache wrapperCache = VillagerOptimizer.getCache();
        Entity entity = player.getTargetEntity(max_distance);
        if (entity == null || !entity.getType().equals(EntityType.VILLAGER)) {
            VillagerOptimizer.getLang(player.locale()).facing_command_optimize_no_villager
                    .forEach(line -> KyoriUtil.sendMessage(player, line));
            return true;
        }
        Villager villager = (Villager) entity;
        Villager.Profession profession = villager.getProfession();
        if (profession.equals(Villager.Profession.NITWIT) || profession.equals(Villager.Profession.NONE)) {
            VillagerOptimizer.getLang(player.locale()).facing_command_optimize_no_villager
                    .forEach(line -> KyoriUtil.sendMessage(player, line));
            return true;
        }

        WrappedVillager wVillager = wrapperCache.get(villager);

        if (wVillager.isOptimized()) {
            VillagerOptimizer.getLang(player.locale()).facing_command_optimize_already_optimized
                    .forEach(line -> KyoriUtil.sendMessage(player, line));
            return true;
        }

        if (player_has_cooldown_bypass || wVillager.canOptimize(cooldown)) {
            VillagerOptimizeEvent optimizeEvent = new VillagerOptimizeEvent(wVillager, OptimizationType.COMMAND, player);
            if (optimizeEvent.callEvent()) {
                wVillager.setOptimizationType(optimizeEvent.getOptimizationType());
                wVillager.saveOptimizeTime();
                VillagerOptimizer.getLang(player.locale()).facing_command_optimize_success
                        .forEach(line -> KyoriUtil.sendMessage(player, line));
            }
        } else {
            final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                    .matchLiteral("%time%")
                    .replacement(Util.formatDuration(Duration.ofMillis(wVillager.getOptimizeCooldownMillis(cooldown))))
                    .build();
            VillagerOptimizer.getLang(player.locale()).facing_command_optimize_cooldown
                    .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(timeLeft)));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return new ArrayList<>(0);
    }
}
