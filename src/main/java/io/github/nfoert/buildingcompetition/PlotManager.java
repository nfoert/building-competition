package io.github.nfoert.buildingcompetition;

import com.sk89q.worldedit.math.BlockVector2;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlotManager {

    private final BuildingCompetition plugin;
    private FileConfiguration plots;
    private File plotsFile;

    public PlotManager(BuildingCompetition plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plotsFile = new File(plugin.getDataFolder(), "plots.yml");

        if (!plotsFile.exists()) {
            plugin.saveResource("plots.yml", false);
        }

        plots = YamlConfiguration.loadConfiguration(plotsFile);
    }

    public void save() throws IOException {
        plots.save(plotsFile);
    }

    public boolean hasPlot(UUID uuid) {
        return plots.contains("plots." + uuid.toString());
    }

    public Location getPlot(UUID uuid, World world) {
        String path = "plots." + uuid;

        int x = plots.getInt(path + ".x");
        int z = plots.getInt(path + ".z");

        return new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
    }

    public void setPlot(UUID uuid, int x, int z) throws IOException {
        String path = "plots." + uuid;

        plots.set(path + ".x", x);
        plots.set(path + ".z", z);

        save();
    }

    public Set<BlockVector2> getUsedPlots() {
        Set<BlockVector2> used = new HashSet<>();

        ConfigurationSection section = plots.getConfigurationSection("plots");
        if (section == null) return used;

        for (String uuid : section.getKeys(false)) {

            int x = plots.getInt("plots." + uuid + ".x");
            int z = plots.getInt("plots." + uuid + ".z");

            used.add(BlockVector2.at(x, z));
        }

        return used;
    }

    public BlockVector2 findNextPlot(Set<BlockVector2> used, int spacing) {
        int x = 0;
        int z = 0;

        int dx = 0;
        int dz = -1;

        int max = 10000;

        for (int i = 0; i < max; i++) {

            int worldX = x * spacing;
            int worldZ = z * spacing;

            BlockVector2 pos = BlockVector2.at(worldX, worldZ);

            if (!used.contains(pos)) {
                return pos;
            }

            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }

            x += dx;
            z += dz;
        }

        throw new RuntimeException("No free plots found");
    }

    public void resetPlots() throws IOException {
        plots.set("plots", null);
        save();
    }
}