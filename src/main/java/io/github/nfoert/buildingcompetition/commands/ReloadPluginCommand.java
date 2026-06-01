package io.github.nfoert.buildingcompetition.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.github.nfoert.buildingcompetition.BuildingCompetition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.configuration.file.FileConfiguration;

import static io.github.nfoert.buildingcompetition.Utils.getUsername;
import static io.github.nfoert.buildingcompetition.Utils.sendMessage;

public class ReloadPluginCommand {
    private final BuildingCompetition plugin;
    private FileConfiguration config;

    public ReloadPluginCommand(BuildingCompetition plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        plugin.reloadConfig();
        config = plugin.getConfig();

        sendMessage(ctx, "<b><dark_aqua>BC:</dark_aqua></b> <green>Configuration reloaded!</green>");
        plugin.getLogger().info("Configuration was reloaded by " + getUsername(ctx));

        return Command.SINGLE_SUCCESS;
    }
}
