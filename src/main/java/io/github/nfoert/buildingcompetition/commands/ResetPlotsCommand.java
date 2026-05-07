package io.github.nfoert.buildingcompetition.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldguard.protection.managers.RegionManager;
import io.github.nfoert.buildingcompetition.BuildingCompetition;
import io.github.nfoert.buildingcompetition.PlotManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static io.github.nfoert.buildingcompetition.Utils.getUsername;
import static io.github.nfoert.buildingcompetition.Utils.sendMessage;

public class ResetPlotsCommand {
    private final BuildingCompetition plugin;
    private final PlotManager plotManager;
    private final RegionManager regionManager;

    public ResetPlotsCommand(BuildingCompetition plugin, PlotManager plotManager, RegionManager regionManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.regionManager = regionManager;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        // Clear plots.yml
        try {
            plotManager.resetPlots();
        } catch (IOException e) {
            e.printStackTrace();

            ctx.getSource().getExecutor().sendRichMessage(
                    "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to reset plots!</red>"
            );
            plugin.getLogger().warning("Failed to reset plots, invoked by " + getUsername(ctx));

            return Command.SINGLE_SUCCESS;
        }

        // Clear WorldGuard regions
        if (regionManager == null) {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>WorldGuard is not available</red>");
            plugin.getLogger().warning("WorldGuard is not available when resetting plots");
            return Command.SINGLE_SUCCESS;
        }

        // Copy keys to avoid concurrent modification
        Set<String> regionIds = new HashSet<>(regionManager.getRegions().keySet());

        try {
            for (String id : regionIds) {
                if (!id.equalsIgnoreCase("__global__")) {
                    regionManager.removeRegion(id);
                }
            }

            // Save changes
            regionManager.save();

            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>Plots reset!</green>");
            plugin.getLogger().info("Plots have been reset by " + getUsername(ctx));
        } catch (Exception e) {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Unable to remove WorldGuard region</red>");
            plugin.getLogger().warning("Unable to remove WorldGuard region when resetting plots");
            e.printStackTrace();
        }

        return Command.SINGLE_SUCCESS;
    }
}