package io.github.nfoert.buildingcompetition;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class BuildingCompetition extends JavaPlugin implements Listener {
    private PlotManager plotManager;

    @Override
    public void onLoad() {
        getServer().getConsoleSender().sendRichMessage("\n\n" +
                "<b><dark_aqua>Building Competition</dark_aqua></b> <dark_gray>(v"+ this.getClass().getPackage().getImplementationVersion() + ")</dark_gray> by <gray>nfoert</gray>\n" +
                "<blue><i>github.com/nfoert/building-competition</i></blue>\n\n" +
                "Please ensure you have <gold>FastAsyncWorldEdit</gold> and <gold>WorldGuard</gold> installed\n"
        );
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        plotManager = new PlotManager(this);

        CommandsHelper commandsHelper = new CommandsHelper(this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(commandsHelper.getCommands());
        });

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Notify the user if plots have been paused
        if (plotManager.getPaused()) {
            event.getPlayer().sendRichMessage(Objects.requireNonNull(this.getConfig().get("player-pause-warning")).toString());
        }
    }
}
