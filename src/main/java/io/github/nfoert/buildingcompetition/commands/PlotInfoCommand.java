package io.github.nfoert.buildingcompetition.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.nfoert.buildingcompetition.BuildingCompetition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static io.github.nfoert.buildingcompetition.Utils.getUsername;
import static io.github.nfoert.buildingcompetition.Utils.sendMessage;

public class PlotInfoCommand {
    private final BuildingCompetition plugin;
    private final RegionManager regionManager;

    public PlotInfoCommand(BuildingCompetition plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        if (regionManager == null) {
            return Command.SINGLE_SUCCESS;
        }

        // Get all regions that are applicable at the given location
        Location playerLocation = ctx.getSource().getExecutor().getLocation();
        ApplicableRegionSet regions = regionManager.getApplicableRegions(BlockVector3.at(playerLocation.x(), playerLocation.y(), playerLocation.z()));

        if (regions.size() > 0) {
            for (ProtectedRegion region : regions) {
                Set<UUID> owners = region.getOwners().getPlayerDomain().getUniqueIds();
                Set<String> usernames = new HashSet<>();

                for (UUID uuid : owners) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    usernames.add(String.format("<yellow>%s <dark_gray>(%s)</dark_gray></yellow>", player.getName(), player.getUniqueId()));
                }
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>Region is owned by " + String.join(", ", usernames) + "</green>");
                plugin.getLogger().info(getUsername(ctx) + " requested plot info");
            }
        } else {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>You're not in a WorldGuard region!</red>");
        }

        return Command.SINGLE_SUCCESS;
    }
}
