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
import com.sk89q.worldedit.math.BlockVector3;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.bukkit.Bukkit.getServer;
import static org.bukkit.Bukkit.getWorld;

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
        LiteralArgumentBuilder<CommandSourceStack> reloadCommand = Commands.literal("reload")
                .executes(ctx -> {
                    this.plugin.reloadConfig();
                    config = this.plugin.getConfig();

                    ctx.getSource().getExecutor().sendRichMessage("<b><dark_aqua>BC:</dark_aqua></b> <green>Configuration reloaded!</green>");

                    return Command.SINGLE_SUCCESS;
                });

        LiteralArgumentBuilder<CommandSourceStack> buildPlotCommand = Commands.literal("buildplot")
                .executes(ctx -> {
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

                    World plotWorld = getWorld(config.getString("plot-world"));

                    if (plotWorld != null) {
                        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(plotWorld))) {
                            editSession.enableQueue();

                            BlockVector3 pasteLocation = BlockVector3.at(
                                    ctx.getSource().getExecutor().getLocation().getBlockX(),
                                    ctx.getSource().getExecutor().getLocation().getBlockY(),
                                    ctx.getSource().getExecutor().getLocation().getBlockZ()
                            );

                            clipboard.paste(editSession, pasteLocation, true);

                            sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>Pasted!</green>");
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

        return root.build();
    }
}
