package io.github.nfoert.buildingcompetition;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.github.nfoert.buildingcompetition.commands.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import static io.github.nfoert.buildingcompetition.Utils.getPlotWorld;
import static io.github.nfoert.buildingcompetition.Utils.getRegionManager;
import static org.bukkit.Bukkit.getWorld;

public class CommandsHelper {
    private final BuildingCompetition plugin;
    private final PlotManager plotManager;
    private FileConfiguration config;

    public CommandsHelper(BuildingCompetition plugin) {
        this.plugin = plugin;
        this.config = this.plugin.getConfig();
        this.plotManager = new PlotManager(plugin);
    }

    /**
     * Registers all the commands
     *
     * @return The commands to register
     */
    public LiteralCommandNode<CommandSourceStack> getCommands() {
        BuildPlotCommand buildPlotCommand = new BuildPlotCommand(this.plugin, getPlotWorld(this.plugin), this.config, this.plotManager, getRegionManager(this.plugin));
        PausePlotsCommand pausePlotsCommand = new PausePlotsCommand(this.plugin, this.plotManager, getRegionManager(this.plugin), this.config);
        PlotInfoCommand plotInfoCommand = new PlotInfoCommand(this.plugin, getRegionManager(this.plugin));
        PluginInfoCommand pluginInfoCommand = new PluginInfoCommand();
        ReloadPluginCommand reloadPluginCommand = new ReloadPluginCommand(this.plugin, this.config);
        ResetPlotsCommand resetPlotsCommand = new ResetPlotsCommand(this.plugin, this.plotManager, getRegionManager(this.plugin));
        UnpausePlotsCommand unpausePlotsCommand = new UnpausePlotsCommand(this.plugin, this.plotManager, getRegionManager(this.plugin));

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("bc").executes(pluginInfoCommand::execute);
        root.then(Commands.literal("buildplot")
                .requires(sender -> sender.getSender().hasPermission("bc.build"))
                .executes(buildPlotCommand::execute)
        );
        root.then(Commands.literal("pause")
                .requires(sender -> sender.getSender().hasPermission("bc.pause"))
                .executes(pausePlotsCommand::execute)
        );
        root.then(Commands.literal("info")
                .requires(sender -> sender.getSender().hasPermission("bc.info"))
                .executes(plotInfoCommand::execute)
        );
        root.then(Commands.literal("reload")
                .requires(sender -> sender.getSender().hasPermission("bc.reload"))
                .executes(reloadPluginCommand::execute)
        );
        root.then(Commands.literal("reset")
                .requires(sender -> sender.getSender().hasPermission("bc.reset"))
                .executes(resetPlotsCommand::execute)
        );
        root.then(Commands.literal("unpause")
                .requires(sender -> sender.getSender().hasPermission("bc.unpause"))
                .executes(unpausePlotsCommand::execute)
        );

        return root.build();
    }
}
