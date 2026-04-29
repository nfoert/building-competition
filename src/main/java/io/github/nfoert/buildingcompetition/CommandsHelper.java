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
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.swing.plaf.synth.Region;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static org.bukkit.Bukkit.*;

public class CommandsHelper {
    private final BuildingCompetition plugin;
    private final PlotManager plotManager;
    private FileConfiguration config;

    public CommandsHelper(BuildingCompetition plugin) {
        this.plugin = plugin;
        this.config = this.plugin.getConfig();
        this.plotManager = new PlotManager(plugin);
    }

    private World getPlotWorld() {
        String worldName = plugin.getConfig().getString("plot-world");
        if (worldName == null) {
            plugin.getLogger().severe("Unable to load the plot world");
            return null;
        } else {
            return getWorld(worldName);
        }
    }

    private RegionManager getRegionManager() {
        World world = getPlotWorld();
        if (world == null) return null;

        RegionContainer container = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer();

        return container.get(BukkitAdapter.adapt(world));
    }

    /**
     * Helper function for sending a rich text message to the executor of the command
     *
     * @param context The command context
     * @param message The message to send to the executor of the command
     */
    private void sendMessage(CommandContext<CommandSourceStack> context, String message) {
        var executor = context.getSource().getExecutor();

        if (executor != null) {
            executor.sendRichMessage(message);
        } else {
            getServer().getConsoleSender().sendRichMessage(message);
        }
    }

    private String getUsername(CommandContext<CommandSourceStack> context) {
        var executor = context.getSource().getExecutor();

        if (executor != null) {
            return executor.getName();
        } else {
            return "N/A";
        }
    }

    /**
     * Reloads the plugin configuration
     *
     * @param ctx The command context
     * @return Command.SINGLE_SUCCESS
     */
    private int reloadPlugin(CommandContext<CommandSourceStack> ctx) {
        this.plugin.reloadConfig();
        config = this.plugin.getConfig();

        sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>Configuration reloaded!</green>");
        plugin.getLogger().info("Configuration was reloaded by " + getUsername(ctx));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Builds a plot for the command executor
     * <br>
     * If the player already has a plot, teleport them there.
     * <br>
     * Otherwise, the schematic file is loaded, pasted into the world, and a WorldGuard region is created.
     * The plot information is stored in `plots.yml`, which includes the center point for later teleporting the player.
     *
     * @param ctx The command context
     * @return Command.SINGLE_SUCCESS
     */
    private int buildPlot(CommandContext<CommandSourceStack> ctx) {
        // Set up
        Entity player = ctx.getSource().getExecutor();
        World plotWorld = getPlotWorld();

        // If a player already has a plot, teleport them there
        if (config.getBoolean("dev") == false) {
            if (plotWorld != null) {
                if (plotManager.hasPlot(player.getUniqueId())) {
                    ctx.getSource().getExecutor().teleport(plotManager.getPlotCenter(player.getUniqueId(), plotWorld));

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
                        plotManager.setPlot(UUID.randomUUID(), x, z, centerX, centerY, centerZ);
                    } else {
                        plotManager.setPlot(ctx.getSource().getExecutor().getUniqueId(), x, z, centerX, centerY, centerZ);
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
                    getRegionManager().addRegion(inner);
                    getRegionManager().save();
                } catch (Exception e) {
                    sendMessage(ctx, "<red>Failed to create regions</red>");
                    plugin.getLogger().warning("Failed to create WorldGuard regions");
                    e.printStackTrace();
                }

                // Teleport to the center of the plot
                ctx.getSource().getExecutor().teleport(
                        new Location(plotWorld, centerX + 0.5, centerY, centerZ + 0.5)
                );

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

    /**
     * Resets `plots.yml` and clears WorldGuard regions
     *
     * @param ctx The command context
     * @return Command.SINGLE_SUCCESS
     */
    private int resetPlots(CommandContext<CommandSourceStack> ctx) {
        // Clear plots.yml
        RegionManager regionManager = getRegionManager();

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

    /**
     * Gets the name of the plot owner, based on the location of the command executor
     *
     * @param ctx The command context
     * @return Command.SINGLE_SUCCESS
     */
    private int plotInfo(CommandContext<CommandSourceStack> ctx) {
        RegionManager regionManager = getRegionManager();

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

    private int pausePlots(CommandContext<CommandSourceStack> ctx) {
        RegionManager regionManager = getRegionManager();

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

    private int unpausePlots(CommandContext<CommandSourceStack> ctx) {
        RegionManager regionManager = getRegionManager();

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

    /**
     * Prints the plugin information and the command documentation.
     *
     * @param ctx The command context
     * @return Command.SINGLE_SUCCESS
     */
    private int pluginInfo(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getExecutor().sendRichMessage("\n" +
                "<b><dark_aqua>Building Competition</dark_aqua></b> by <gray>nfoert</gray>\n" +
                "<click:open_url:'https://github.com/nfoert/building-competition'><blue><i>github.com/nfoert/building-competition</i></blue></click>\n");
        ctx.getSource().getExecutor().sendRichMessage(
                "<dark_aqua>Available commands:</dark_aqua>\n" +
                        "\n" +
                        "<aqua>/bc reload</aqua> <gray>- Reloads the configuration</gray>\n" +
                        "<aqua>/bc buildplot</aqua> <gray>- Builds a plot for the sender</gray>\n" +
                        "<aqua>/bc reset</aqua> <gray>- Resets the plot file</gray>\n" +
                        "<aqua>/bc info</aqua> <gray>- Get info for a plot, based on where you're standing</gray>\n" +
                        "<aqua>/bc pause</aqua> <gray>- Disable plot building</gray>\n" +
                        "<aqua>/bc unpause</aqua> <gray>- Enable plot building</gray>\n"
        );

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Registers all the commands
     *
     * @return The commands to register
     */
    public LiteralCommandNode<CommandSourceStack> getCommands() {
        LiteralArgumentBuilder<CommandSourceStack> reloadCommand = Commands.literal("reload").requires(sender -> sender.getSender().hasPermission("bc.reload"))
                .executes(ctx -> reloadPlugin(ctx));

        LiteralArgumentBuilder<CommandSourceStack> buildPlotCommand = Commands.literal("buildplot").requires(sender -> sender.getSender().hasPermission("bc.build"))
                .executes(ctx -> buildPlot(ctx));

        LiteralArgumentBuilder<CommandSourceStack> resetPlotsCommand = Commands.literal("reset").requires(sender -> sender.getSender().hasPermission("bc.reset"))
                .executes(ctx -> resetPlots(ctx));

        LiteralArgumentBuilder<CommandSourceStack> plotInfoCommand = Commands.literal("info").requires(sender -> sender.getSender().hasPermission("bc.info"))
                .executes(ctx -> plotInfo(ctx));

        LiteralArgumentBuilder<CommandSourceStack> plotPauseCommand = Commands.literal("pause").requires(sender -> sender.getSender().hasPermission("bc.pause"))
                .executes(ctx -> pausePlots(ctx));

        LiteralArgumentBuilder<CommandSourceStack> plotUnpauseCommand = Commands.literal("unpause").requires(sender -> sender.getSender().hasPermission("bc.unpause"))
                .executes(ctx -> unpausePlots(ctx));

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("bc").executes(ctx -> pluginInfo(ctx));

        root.then(reloadCommand);
        root.then(buildPlotCommand);
        root.then(resetPlotsCommand);
        root.then(plotInfoCommand);
        root.then(plotPauseCommand);
        root.then(plotUnpauseCommand);

        return root.build();
    }
}
