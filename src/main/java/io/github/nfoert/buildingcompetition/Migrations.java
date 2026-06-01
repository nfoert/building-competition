package io.github.nfoert.buildingcompetition;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static io.github.nfoert.buildingcompetition.Utils.getRegionManager;

public class Migrations {
    private final Plugin plugin;
    private final PlotManager plotManager;

    public Migrations(Plugin plugin, PlotManager plotManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
    }

    public void v1() {
        plugin.getLogger().info("Migrating plots.yml to version 1...");
        // Add support for teleportX and teleportZ
        RegionManager regionManager = getRegionManager(plugin);

        if (regionManager == null) {
            plugin.getLogger().severe("Unable to load the region manager");
            return;
        }

        Set<String> regionIds = new HashSet<>(regionManager.getRegions().keySet());

        try {
            for (String id : regionIds) {
                if (id.equalsIgnoreCase("__global__")) {
                    continue;
                }
                if (!id.endsWith("_build")) {
                    plugin.getLogger().warning("Unable to migrate plot " + id + " to version 1");
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
                plugin.getLogger().info("Successfully migrated plots.yml to version 1");
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to set version in plots.yml");
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to migrate plots.yml to version 1");
            e.printStackTrace();
        }
    }
}
