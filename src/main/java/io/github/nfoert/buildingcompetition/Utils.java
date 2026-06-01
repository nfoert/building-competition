package io.github.nfoert.buildingcompetition;

import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import static org.bukkit.Bukkit.getServer;
import static org.bukkit.Bukkit.getWorld;

public class Utils {
    /**
     * Helper function for sending a rich text message to the executor of the command
     *
     * @param context The command context
     * @param message The message to send to the executor of the command
     */
    public static void sendMessage(CommandContext<CommandSourceStack> context, String message) {
        var executor = context.getSource().getExecutor();

        if (executor != null) {
            executor.sendRichMessage(message);
        } else {
            getServer().getConsoleSender().sendRichMessage(message);
        }
    }

    /**
     * Get the username of the command executor
     * If the command executor is not a player, instead return N/A
     *
     * @param context The command context
     * @return The username or N/A
     */
    public static String getUsername(CommandContext<CommandSourceStack> context) {
        var executor = context.getSource().getExecutor();

        if (executor != null) {
            return executor.getName();
        } else {
            return "N/A";
        }
    }

    /**
     * Get the plot world
     *
     * @param plugin The plugin instance
     * @return The plot world
     */
    public static World getPlotWorld(Plugin plugin) {
        String worldName = plugin.getConfig().getString("plot-world");
        if (worldName == null) {
            plugin.getLogger().severe("Unable to load the plot world");
            return null;
        } else {
            return getWorld(worldName);
        }
    }

    /**
     * Get the region manager
     *
     * @param plugin The plugin instance
     * @return The region manager
     */
    public static RegionManager getRegionManager(Plugin plugin) {
        World world = getPlotWorld(plugin);
        if (world == null) return null;

        RegionContainer container = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer();

        return container.get(BukkitAdapter.adapt(world));
    }
}
