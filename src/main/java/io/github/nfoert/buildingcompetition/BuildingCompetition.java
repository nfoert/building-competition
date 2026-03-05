package io.github.nfoert.buildingcompetition;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;

public final class BuildingCompetition extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            double angle = 0;

            @Override
            public void run() {
                BlockData block = getServer().createBlockData(Material.STONE);

                for (Player player : Bukkit.getOnlinePlayers()) {

                    Location loc = player.getLocation().clone().add(0, 1, 0);
                    Location block_loc = player.getLocation().clone().add(0, 3, 0);

                    double radius = 0.7;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);

                    loc.add(x, 0, z);

                    player.getWorld().spawnParticle(
                            Particle.HAPPY_VILLAGER,
                            loc,
                            1,
                            0, 0, 0,
                            0
                    );

                    player.getWorld().setBlockData(block_loc, block);
                }

                angle += 0.1;
            }

        }.runTaskTimer(this, 0L, 1L); // 1 tick (20 times per second)
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
        event.getPlayer().sendMessage(Component.text("Look out, this is a custom message sent from a plugin"));

        ItemStack item = ItemStack.of(Material.STICK, 10);

        event.getPlayer().getInventory().setItem(0, item);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.STICK) {
            event.getPlayer().sendMessage(Component.text("whyd you do that :("));
            event.getPlayer().getInventory().addItem(ItemStack.of(Material.STICK, 1));
        }
    }

    @EventHandler
    public void onPlayerHurt(EntityDamageEvent event) {
        if (event.getDamageSource().getDamageType() == DamageType.GENERIC) {
            if (event.getDamageSource().getCausingEntity() instanceof LivingEntity) {
                ((LivingEntity) event.getDamageSource().getCausingEntity()).damage(2);
            }
        }
    }
}
