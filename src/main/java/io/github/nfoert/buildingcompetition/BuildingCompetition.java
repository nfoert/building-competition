package io.github.nfoert.buildingcompetition;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

import static io.github.nfoert.buildingcompetition.Utils.getPlotWorld;
import static io.github.nfoert.buildingcompetition.Utils.getRegionManager;

public final class BuildingCompetition extends JavaPlugin implements Listener {
    private PlotManager plotManager;
    private final int KEEP_IN_PLOT_DISTANCE = getConfig().getInt("keep-in-plot-distance", 16);

    private boolean isInsidePlot(Player player, Location location) {
        RegionManager regionManager = getRegionManager(this);

        if (regionManager == null) {
            return true;
        }

        ProtectedRegion region =
                regionManager.getRegion(player.getUniqueId() + "_build");

        if (region == null) {
            return true;
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        int x = location.getBlockX();
        int z = location.getBlockZ();

        return x >= min.x() - KEEP_IN_PLOT_DISTANCE
                && x <= max.x() + KEEP_IN_PLOT_DISTANCE
                && z >= min.z() - KEEP_IN_PLOT_DISTANCE
                && z <= max.z() + KEEP_IN_PLOT_DISTANCE;
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
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        // Ignore tiny movements
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isOp()) return;
        if (player.hasPermission("bc.ignore-keep-in-plot")) return;
        if (!(player.getWorld() == getPlotWorld(this))) return;

        if (!isInsidePlot(player, to)) {
            event.setTo(from);
            player.sendRichMessage("<b><dark_aqua>BC:</dark_aqua></b> <yellow>You cannot leave your plot.</yellow>");
        }
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        Migrations migrations = new Migrations(this, plotManager);

        // Migration to add support for teleportX and teleportZ to the plots.yml file
        if (plotManager.getVersion() == 0) {
            migrations.v1();
        }
    }
}
