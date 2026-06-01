package io.github.nfoert.buildingcompetition.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.nfoert.buildingcompetition.BuildingCompetition;
import io.github.nfoert.buildingcompetition.PlotManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static io.github.nfoert.buildingcompetition.Utils.getUsername;
import static io.github.nfoert.buildingcompetition.Utils.sendMessage;
import static org.bukkit.Bukkit.getOnlinePlayers;

public class PausePlotsCommand {
    private final BuildingCompetition plugin;
    private final PlotManager plotManager;
    private final RegionManager regionManager;
    private final FileConfiguration config;

    public PausePlotsCommand(BuildingCompetition plugin, PlotManager plotManager, RegionManager regionManager, FileConfiguration config) {
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.regionManager = regionManager;
        this.config = config;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        try {
            if (plotManager.getPaused()) {
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <yellow>Plots are already paused!</yellow>");
                plugin.getLogger().info(getUsername(ctx) + " tried to pause plots, but they were already paused.");
            } else {
                // Set state in plot manager
                plotManager.setPaused(true);

                // Disable building in WorldGuard regions in the plot world
                Set<String> regionIds = new HashSet<>(regionManager.getRegions().keySet());

                try {
                    for (String id : regionIds) {
                        if (!id.equalsIgnoreCase("__global__")) {
                            ProtectedRegion region = regionManager.getRegion(id);
                            region.setFlag(Flags.BUILD, StateFlag.State.DENY);
                        }
                    }

                    // Save changes
                    regionManager.save();
                } catch (Exception e) {
                    sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Unable to disable building in worldguard region</red>");
                    plugin.getLogger().warning(getUsername(ctx) + " tried to pause plots, but there was a problem setting DENY on the BUILD flag for a region");
                    e.printStackTrace();
                }

                // Notify players
                Collection<? extends Player> players = getOnlinePlayers();

                for (Player player: players) {
                    player.sendRichMessage(Objects.requireNonNull(config.get("player-pause-warning")).toString());
                }

                plugin.getLogger().info(getUsername(ctx) + " paused plots");
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>Plots have been paused!</green>");
            }
        } catch (IOException e) {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to pause plots!</red>");
            plugin.getLogger().warning(getUsername(ctx) + "  failed to set the paused flag to the PlotManager");
        }

        return Command.SINGLE_SUCCESS;
    }
}