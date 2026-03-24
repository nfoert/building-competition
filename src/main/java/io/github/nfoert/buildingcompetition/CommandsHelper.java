package io.github.nfoert.buildingcompetition;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
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
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.Bukkit.*;

public class CommandsHelper {

    private final BuildingCompetition plugin;
    private FileConfiguration config;

    public CommandsHelper(BuildingCompetition plugin) {
        this.plugin = plugin;

        config = this.plugin.getConfig();
    }

    private void sendMessage(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().getExecutor().sendRichMessage(message);
    }

    private int reloadPlugin(CommandContext<CommandSourceStack> ctx) {
        this.plugin.reloadConfig();
        config = this.plugin.getConfig();

        ctx.getSource().getExecutor().sendRichMessage("<b><dark_aqua>BC:</dark_aqua></b> <green>Configuration reloaded!</green>");

        return Command.SINGLE_SUCCESS;
    }

    private int buildPlot(CommandContext<CommandSourceStack> ctx) {
        // Set up
        PlotManager plotManager = new PlotManager(plugin);
        World plotWorld = getWorld(config.getString("plot-world"));
        Entity player = ctx.getSource().getExecutor();

        // If a player already has a plot, teleport them there
        if (config.getBoolean("dev") == false) {
            if (plotWorld != null) {
                if (plotManager.hasPlot(player.getUniqueId())) {
                    ctx.getSource().getExecutor().teleport(plotManager.getPlotCenter(player.getUniqueId(), plotWorld));

                    sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>You've been teleported to your existing plot!</green>");
                    return Command.SINGLE_SUCCESS;
                }
            } else {
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Unable to find world!</red>");
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
                return Command.SINGLE_SUCCESS;
            }
        } else {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to load schematic</red>");
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

                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                RegionManager regionManager = container.get(BukkitAdapter.adapt(plotWorld));

                // Save plot data
                try {
                    if (config.getBoolean("dev")) {
                        plotManager.setPlot(UUID.randomUUID(), x, z, centerX, centerY, centerZ);
                    } else {
                        plotManager.setPlot(ctx.getSource().getExecutor().getUniqueId(), x, z, centerX, centerY, centerZ);
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    ctx.getSource().getExecutor().sendRichMessage(
                            "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to save plot data!</red>"
                    );

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
                String baseId = player.getUniqueId().toString();

                ProtectedRegion inner = new ProtectedCuboidRegion(
                        baseId + "_build",
                        innerMin,
                        innerMax
                );

                // No priority needed anymore (no conflict region)
                inner.setFlag(Flags.BUILD, StateFlag.State.ALLOW);

                // Ownership
                inner.getOwners().addPlayer(player.getUniqueId());

                try {
                    regionManager.addRegion(inner);
                    regionManager.save();
                } catch (Exception e) {
                    sendMessage(ctx, "<red>Failed to create regions</red>");
                    e.printStackTrace();
                }

                // Teleport to the center of the plot
                ctx.getSource().getExecutor().teleport(
                        new Location(plotWorld, centerX + 0.5, centerY, centerZ + 0.5)
                );

                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>You've been teleported to your plot!</green>");
            } catch (WorldEditException e) {
                e.printStackTrace();
                sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to paste schematic</red>");
                return Command.SINGLE_SUCCESS;
            }
        } else {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Unable to find world!</red>");
            return Command.SINGLE_SUCCESS;
        }

        return Command.SINGLE_SUCCESS;
    }

    private int resetPlots(CommandContext<CommandSourceStack> ctx) {
        // Clear plots.yml
        PlotManager plotManager = new PlotManager(plugin);
        World plotWorld = getWorld(config.getString("plot-world"));

        try {
            plotManager.resetPlots();
        } catch (IOException e) {
            e.printStackTrace();

            ctx.getSource().getExecutor().sendRichMessage(
                    "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to reset plots!</red>"
            );

            return Command.SINGLE_SUCCESS;
        }

        // Clear WorldGuard regions
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(plotWorld));

        if (regionManager == null) {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>WorldGuard is not available</red>");
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
        } catch (Exception e) {
            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <red>Unable to remove WorldGuard region</red>");
            e.printStackTrace();
        }

        ctx.getSource().getExecutor().sendRichMessage("<b><dark_aqua>BC:</dark_aqua></b> <green>Plots reset!</green>");

        return Command.SINGLE_SUCCESS;
    }

    private int pluginInfo(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getExecutor().sendRichMessage("\n" +
                "<b><dark_aqua>Building Competition</dark_aqua></b> by <gray>nfoert</gray>\n" +
                "<click:open_url:'https://github.com/nfoert/building-competition'><blue><i>github.com/nfoert/building-competition</i></blue></click>\n");
        ctx.getSource().getExecutor().sendRichMessage(
                "<dark_aqua>Available commands:</dark_aqua>\n" +
                        "\n" +
                        "<aqua>/bc reload</aqua> <gray>- Reloads the configuration</gray>\n" +
                        "<aqua>/bc buildplot</aqua> <gray>- Builds a plot for the sender</gray>\n" +
                        "<aqua>/bc reset</aqua> <gray>- Resets the plot file</gray>\n"
        );

        return Command.SINGLE_SUCCESS;
    }

    public LiteralCommandNode<CommandSourceStack> getCommands() {
        LiteralArgumentBuilder<CommandSourceStack> reloadCommand = Commands.literal("reload").requires(sender -> sender.getSender().hasPermission("bc.reload"))
                .executes(ctx -> reloadPlugin(ctx));

        LiteralArgumentBuilder<CommandSourceStack> buildPlotCommand = Commands.literal("buildplot").requires(sender -> sender.getSender().hasPermission("bc.build"))
                .executes(ctx -> buildPlot(ctx));

        LiteralArgumentBuilder<CommandSourceStack> resetPlotsCommand = Commands.literal("reset").requires(sender -> sender.getSender().hasPermission("bc.reset"))
                .executes(ctx -> resetPlots(ctx));

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("bc").executes(ctx -> pluginInfo(ctx));

        root.then(reloadCommand);
        root.then(buildPlotCommand);
        root.then(resetPlotsCommand);

        return root.build();
    }
}
