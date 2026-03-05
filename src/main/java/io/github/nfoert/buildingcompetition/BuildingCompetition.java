package io.github.nfoert.buildingcompetition;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class BuildingCompetition extends JavaPlugin implements Listener {
    @Override
    public void onLoad() {
        getServer().sendRichMessage("\n\n" +
                "<b><dark_aqua>Building Competition</dark_aqua></b> by <gray>nfoert</gray>\n" +
                "<blue><i>github.com/nfoert/building-competition</i></blue>\n");
    }

    @Override
    public void onEnable() {
        CommandsHelper commandsHelper = new CommandsHelper();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(commandsHelper.getCommands(getDataFolder()));
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Player join logic
    }
}
