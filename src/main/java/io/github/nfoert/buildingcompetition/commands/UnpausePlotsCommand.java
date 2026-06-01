package io.github.nfoert.buildingcompetition.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.nfoert.buildingcompetition.BuildingCompetition;
import io.github.nfoert.buildingcompetition.PlotManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static io.github.nfoert.buildingcompetition.Utils.getUsername;
import static io.github.nfoert.buildingcompetition.Utils.sendMessage;

public class UnpausePlotsCommand {
    private final BuildingCompetition plugin;
    private final PlotManager plotManager;
    private final RegionManager regionManager;

    public UnpausePlotsCommand(BuildingCompetition plugin, PlotManager plotManager, RegionManager regionManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.regionManager = regionManager;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        try {
            if (plotManager.getPaused()) {
                // Set state in plot manager
                plotManager.setPaused(false);

                // Disable building in WorldGuard regions in the plot world
                Set<String> regionIds = new HashSet<>(regionManager.getRegions().keySet());

                try {
                    for (String id : regionIds) {
                        if (!id.equalsIgnoreCase("__global__")) {
                            ProtectedRegion region = regionManager.getRegion(id);
                            region.setFlag(Flags.BUILD, null);
                        }
                    }

                    // Save changes
                    regionManager.save();
                } catch (Exception e) {
                    sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Unable to enable building in worldguard region</red>");
                    plugin.getLogger().warning(getUsername(ctx) + " tried to pause plots, but there was a problem setting null on the BUILD flag for a region");
                    e.printStackTrace();
                }

                // Notify players
                //    Nothing is sent to all players when unpausing
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>Plots have been unpaused!</green>");
            } else {
                plugin.getLogger().info(getUsername(ctx) + " tried to unpause plots, but they were already unpaused.");
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <yellow>Plots are already not paused!</yellow>");
            }
        } catch (IOException e) {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to unpause plots!</red>");
            plugin.getLogger().warning(getUsername(ctx) + "  failed to set the paused flag to the PlotManager");
        }

        return Command.SINGLE_SUCCESS;
    }
}