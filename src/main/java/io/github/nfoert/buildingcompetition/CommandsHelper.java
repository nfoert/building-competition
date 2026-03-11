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
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    public LiteralCommandNode<CommandSourceStack> getCommands() {
        LiteralArgumentBuilder<CommandSourceStack> reloadCommand = Commands.literal("reload").requires(sender -> sender.getSender().hasPermission("bc.reload"))
                .executes(ctx -> {
                    this.plugin.reloadConfig();
                    config = this.plugin.getConfig();

                    ctx.getSource().getExecutor().sendRichMessage("<b><dark_aqua>BC:</dark_aqua></b> <green>Configuration reloaded!</green>");

                    return Command.SINGLE_SUCCESS;
                });

        LiteralArgumentBuilder<CommandSourceStack> buildPlotCommand = Commands.literal("buildplot").requires(sender -> sender.getSender().hasPermission("bc.build"))
                .executes(ctx -> {
                    // Set up
                    PlotManager plotManager = new PlotManager(plugin);
                    World plotWorld = getWorld(config.getString("plot-world"));
                    Entity player = ctx.getSource().getExecutor();

                    // If a player already has a plot, teleport them there
                    if (config.getBoolean("dev") == false) {
                        if (plotWorld != null) {
                            if (plotManager.hasPlot(player.getUniqueId())) {
                                player.teleport(plotManager.getPlot(player.getUniqueId(), plotWorld));
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

                    try {
                        if (config.getBoolean("dev") == true) {
                            plotManager.setPlot(UUID.randomUUID(), x, z);
                        } else {
                            plotManager.setPlot(ctx.getSource().getExecutor().getUniqueId(), x, z);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();

                        ctx.getSource().getExecutor().sendRichMessage(
                                "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to save plot data!</red>"
                        );

                        return Command.SINGLE_SUCCESS;
                    }

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

                            ctx.getSource().getExecutor().teleport(new Location(plotWorld, x + 0.5, 0 + 1, z + 0.5));

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
                });

        LiteralArgumentBuilder<CommandSourceStack> resetPlotsCommand = Commands.literal("reset").requires(sender -> sender.getSender().hasPermission("bc.reset"))
                .executes(ctx -> {
                    PlotManager plotManager = new PlotManager(plugin);

                    try {
                        plotManager.resetPlots();
                    } catch (IOException e) {
                        e.printStackTrace();

                        ctx.getSource().getExecutor().sendRichMessage(
                                "<b><dark_aqua>BC:</dark_aqua></b> <red>Failed to reset plots!</red>"
                        );

                        return Command.SINGLE_SUCCESS;
                    }

                    ctx.getSource().getExecutor().sendRichMessage("<b><dark_aqua>BC:</dark_aqua></b> <green>Plots reset!</green>");

                    return Command.SINGLE_SUCCESS;
                });

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("bc").executes(ctx -> {
                    ctx.getSource().getExecutor().sendRichMessage("\n" +
                            "<b><dark_aqua>Building Competition</dark_aqua></b> by <gray>nfoert</gray>\n" +
                            "<click:open_url:'https://github.com/nfoert/building-competition'><blue><i>github.com/nfoert/building-competition</i></blue></click>\n");
                    ctx.getSource().getExecutor().sendRichMessage("<dark_aqua>Available commands:</dark_aqua>\n" +
                            "\n" +
                            "<aqua>/bc reload</aqua> <gray>- Reloads the configuration</gray>");

                    return Command.SINGLE_SUCCESS;
                });

        root.then(reloadCommand);
        root.then(buildPlotCommand);
        root.then(resetPlotsCommand);

        return root.build();
    }
}
