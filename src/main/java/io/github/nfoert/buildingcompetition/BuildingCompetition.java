package io.github.nfoert.buildingcompetition;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static io.github.nfoert.buildingcompetition.Utils.getUsername;
import static io.github.nfoert.buildingcompetition.Utils.sendMessage;
import static org.bukkit.Bukkit.getWorld;

public final class BuildingCompetition extends JavaPlugin implements Listener {
    private PlotManager plotManager;

    // TODO: Unify with equivalent in CommandsHelper
    private World getPlotWorld() {
        String worldName = getConfig().getString("plot-world");
        if (worldName == null) {
            getLogger().severe("Unable to load the plot world");
            return null;
        } else {
            return getWorld(worldName);
        }
    }

    // TODO: Unify with equivalent in CommandsHelper
    private RegionManager getRegionManager() {
        World world = getPlotWorld();
        if (world == null) return null;

        RegionContainer container = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer();

        return container.get(BukkitAdapter.adapt(world));
    }

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

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        // Migration to add support for teleportX and teleportZ to the plots.yml file
        if (plotManager.getVersion() == 0) {
            getLogger().info("Migrating plots.yml to version 1...");
            // Add support for teleportX and teleportZ
            RegionManager regionManager = getRegionManager();

            if (regionManager == null) {
                getLogger().severe("Unable to load the region manager");
                return;
            }

            Set<String> regionIds = new HashSet<>(regionManager.getRegions().keySet());

            try {
                for (String id : regionIds) {
                    if (id.equalsIgnoreCase("__global__")) {
                        continue;
                    }
                    if (!id.endsWith("_build")) {
                        getLogger().warning("Unable to migrate plot " + id + " to version 1");
                        continue;
                    }

                    ProtectedRegion region = regionManager.getRegion(id);
                    // Set the teleport location to one block outside of the plot
                    // TODO: Have more robust UUID parsing
                    plotManager.setTeleportLocation(
                            UUID.fromString(id.replace("_build", "")),
                            region.getMinimumPoint().x() - 1,
                            region.getMinimumPoint().y() + 1,
                            region.getMinimumPoint().z() - 1
                    );
                }

                // Set version in plots.yml
                try {
                    plotManager.setVersion(1);
                    getLogger().info("Successfully migrated plots.yml to version 1");
                } catch (IOException e) {
                    getLogger().warning("Failed to set version in plots.yml");
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                getLogger().warning("Failed to migrate plots.yml to version 1");
                e.printStackTrace();
            }
        }
    }
}
