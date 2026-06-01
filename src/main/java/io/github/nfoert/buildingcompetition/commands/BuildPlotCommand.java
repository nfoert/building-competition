package io.github.nfoert.buildingcompetition.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.nfoert.buildingcompetition.BuildingCompetition;
import io.github.nfoert.buildingcompetition.PlotManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static io.github.nfoert.buildingcompetition.Utils.getUsername;
import static io.github.nfoert.buildingcompetition.Utils.sendMessage;

public class BuildPlotCommand {
    private final BuildingCompetition plugin;
    private World plotWorld;
    private final FileConfiguration config;
    private final PlotManager plotManager;
    private final RegionManager regionManager;

    public BuildPlotCommand(BuildingCompetition plugin, World plotWorld, FileConfiguration config, PlotManager plotManager, RegionManager regionManager) {
        this.plugin = plugin;
        this.plotWorld = plotWorld;
        this.config = config;
        this.plotManager = plotManager;
        this.regionManager = regionManager;
    }

    /**
     * Checks whether a location is safe for a player to stand in.
     *
     * @param location The location to check
     * @return If it's a safe position to teleport to or not
     */
    private boolean isSafeTeleportLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);

        return feet.isPassable()
                && head.isPassable()
                && ground.getType().isSolid();
    }

    /**
     * Teleport a player to their plot.
     * Try to teleport in the center of their build. If that location is obstructed, teleport to the corner of their plot instead.
     *
     * @param player The player to teleport
     * @param plotWorld The world to teleport the player in
     */
    private void teleportToPlot(Entity player, World plotWorld) {
        UUID uuid = player.getUniqueId();

        Location center = plotManager.getPlotCenter(uuid, plotWorld);

        if (isSafeTeleportLocation(center)) {
            player.teleport(center);
        } else {
            Location corner = plotManager.getCornerTeleportLocation(uuid, plotWorld);
            player.teleport(corner);
        }
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        // Set up
        Entity player = ctx.getSource().getExecutor();

        // If a player already has a plot, teleport them there
        if (config.getBoolean("dev") == false) {
            if (plotWorld != null) {
                if (plotManager.hasPlot(player.getUniqueId())) {
                    teleportToPlot(ctx.getSource().getExecutor(), plotWorld);

                    sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>You've been teleported to your existing plot!</green>");
                    plugin.getLogger().info(getUsername(ctx) + " was sent to their existing plot");
                    return Command.SINGLE_SUCCESS;
                }
            } else {
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Unable to find world!</red>");
                plugin.getLogger().warning("Unable to find plot world when trying to teleport user to plot");
                return Command.SINGLE_SUCCESS;
            }
        }

        sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <aqua>Building plot for " + ctx.getSource().getExecutor().getName() + "...</aqua>");

        // Load schematic
        File schematicFile = new File(
                plugin.getDataFolder(),
                config.getString("schem-file")
        );

        Clipboard clipboard = null;
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

        if (format != null) {
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                clipboard = reader.read();
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to load schematic</red>");
                plugin.getLogger().warning("Failed to load schematic");
                return Command.SINGLE_SUCCESS;
            }
        } else {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to load schematic</red>");
            plugin.getLogger().warning("Failed to load schematic");
            return Command.SINGLE_SUCCESS;
        }

        // Figure out where to place the schematic
        Set<BlockVector2> used = plotManager.getUsedPlots();

        int spacing = clipboard.getWidth() + 16;

        BlockVector2 plot = plotManager.findNextPlot(used, spacing);

        int x = plot.x();
        int z = plot.z();

        // Place the schematic
        if (plotWorld != null) {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(plotWorld))) {
                editSession.enableQueue();

                BlockVector3 pasteLocation = BlockVector3.at(
                        x,
                        0,
                        z
                );

                clipboard.paste(editSession, pasteLocation, true);

                // Add WorldGuard exception to plot location
                BlockVector3 min = clipboard.getMinimumPoint();
                BlockVector3 max = clipboard.getMaximumPoint();
                BlockVector3 origin = clipboard.getOrigin();
                BlockVector3 minPoint = pasteLocation.add(min.subtract(origin));
                BlockVector3 maxPoint = pasteLocation.add(max.subtract(origin));

                double centerX = (minPoint.x() + maxPoint.x()) / 2.0;
                double centerZ = (minPoint.z() + maxPoint.z()) / 2.0;
                double centerY = minPoint.y() + 2;

                // Save plot data
                try {
                    if (config.getBoolean("dev")) {
                        plotManager.setPlot(UUID.randomUUID(), x, z, centerX, centerY, centerZ, minPoint.x() - 1, minPoint.y() + 1, minPoint.z() - 1);
                    } else {
                        plotManager.setPlot(ctx.getSource().getExecutor().getUniqueId(), x, z, centerX, centerY, centerZ, minPoint.x() - 1, minPoint.y() + 1, minPoint.z() - 1);
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    ctx.getSource().getExecutor().sendRichMessage(
                            "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to save plot data!</red>"
                    );
                    plugin.getLogger().warning("Failed to save plot data");

                    return Command.SINGLE_SUCCESS;
                }

                //    Find corner blocks
                BlockVector3 redstone1 = null;
                BlockVector3 redstone2 = null;

                for (int cornerX = min.x(); cornerX <= max.x(); cornerX++) {
                    for (int cornerY = min.y(); cornerY <= max.y(); cornerY++) {
                        for (int cornerZ = min.z(); cornerZ <= max.z(); cornerZ++) {
                            BlockVector3 pos = BlockVector3.at(cornerX, cornerY, cornerZ);
                            BlockState block = clipboard.getBlock(pos);

                            if (block.getBlockType().id().equals(config.getString("build-area-corner"))) {
                                if (redstone1 == null) {
                                    redstone1 = pos;
                                } else {
                                    redstone2 = pos;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (redstone1 == null || redstone2 == null) {
                    sendMessage(ctx, "<red>Schematic must contain exactly 2 redstone blocks!</red>");
                    plugin.getLogger().warning("Schematic validation failed: It must contain exactly 2 of build-area-corner block");
                    return Command.SINGLE_SUCCESS;
                }

                BlockVector3 worldRed1 = pasteLocation.add(redstone1.subtract(origin));
                BlockVector3 worldRed2 = pasteLocation.add(redstone2.subtract(origin));

                //    Get region heights
                int buildHeight = config.getInt("build-height", 100);

                // Base Y = redstone block Y + 1 (start ABOVE floor)
                int baseY = Math.min(worldRed1.y(), worldRed2.y()) + 1;

                BlockVector3 innerMin = BlockVector3.at(
                        Math.min(worldRed1.x(), worldRed2.x()),
                        baseY,
                        Math.min(worldRed1.z(), worldRed2.z())
                );

                BlockVector3 innerMax = BlockVector3.at(
                        Math.max(worldRed1.x(), worldRed2.x()),
                        baseY + buildHeight,
                        Math.max(worldRed1.z(), worldRed2.z())
                );

                //    Create regions
                String baseId = "";

                if (config.getBoolean("dev")) {
                    baseId = UUID.randomUUID().toString();
                } else {
                    baseId = player.getUniqueId().toString();
                }

                ProtectedRegion inner = new ProtectedCuboidRegion(
                        baseId + "_build",
                        innerMin,
                        innerMax
                );

                // No priority needed anymore (no conflict region)
                if (!plotManager.getPaused()) {
                    inner.setFlag(Flags.BUILD, null);
                } else {
                    inner.setFlag(Flags.BUILD, StateFlag.State.DENY);
                    sendMessage(ctx, Objects.requireNonNull(config.get("player-pause-warning")).toString());
                    plugin.getLogger().info("Plots are paused, so the BUILD flag is DENY when creating plot for " + getUsername(ctx));
                }

                // Ownership
                inner.getOwners().addPlayer(player.getUniqueId());

                try {
                    regionManager.addRegion(inner);
                    regionManager.save();
                } catch (Exception e) {
                    sendMessage(ctx, "<red>Failed to create regions</red>");
                    plugin.getLogger().warning("Failed to create WorldGuard regions");
                    e.printStackTrace();
                }

                // Teleport to the center of the plot
                teleportToPlot(ctx.getSource().getExecutor(), plotWorld);

                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>You've been teleported to your plot!</green>");
                plugin.getLogger().info("Created plot for " + getUsername(ctx));
            } catch (WorldEditException e) {
                e.printStackTrace();
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to paste schematic</red>");
                plugin.getLogger().warning("Failed to paste schematic");
                return Command.SINGLE_SUCCESS;
            }
        } else {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Unable to find world!</red>");
            plugin.getLogger().warning("Failed to find plot world");
            return Command.SINGLE_SUCCESS;
        }

        return Command.SINGLE_SUCCESS;
    }
}