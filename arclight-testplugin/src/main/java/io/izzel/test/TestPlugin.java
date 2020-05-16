package io.izzel.test;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        getCommand("test").setExecutor(this);
        getServer().getScheduler().runTaskTimer(this, () -> {
            getServer().getConsoleSender().sendMessage(getConfig().getString("text", ""));
        }, 0, getConfig().getInt("period", 100));
        getServer().getPluginManager().registerEvents(new Test(), this);
        try {
            Field console = getServer().getClass().getDeclaredField("console");
            console.setAccessible(true);
            Object server = console.get(getServer());
            getLogger().info(server.getClass().toString());
            getLogger().info(server.getClass().getName());
            getLogger().info(server.getClass().getSimpleName());
            Field field = server.getClass().getDeclaredField("remoteStatusListener"); // rconQueryThread
            getLogger().info(field.toString());
            getLogger().info(field.getName());
            Method method = server.getClass().getMethod("aW"); // loadResourcePackSHA
            getLogger().info(method.toString());
            getLogger().info(method.getName());
            Class<?> cl = Class.forName("net.minecraft.server.v1_14_R1.World");
            for (Constructor<?> constructor : cl.getDeclaredConstructors()) {
                getLogger().info(constructor.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.BLUE + getConfig().getString("text", ""));
        return true;
    }

    private static class Test implements Listener {

        @EventHandler
        public void on(BlockBreakEvent event) {
            event.getPlayer().sendMessage("breaking " + event.getBlock());
            if (event.getBlock().getType() == Material.STONE) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("cancelled");
            }
            if (event.getBlock().getType() == Material.COAL_ORE) {
                event.setExpToDrop(1000);
                event.getPlayer().sendMessage("set exp to 1000");
            }
        }

        @EventHandler
        public void on(BlockDropItemEvent event) {
            event.getPlayer().sendMessage("dropping " + event.getItems());
            if (event.getItems().size() > 1) {
                event.getPlayer().sendMessage("removing second item drop");
                event.getItems().remove(1);
            }
        }

        @EventHandler
        public void on(EntityExplodeEvent event) {
            Bukkit.broadcastMessage("exploding " + event.getEntity());
            event.setCancelled(true);
        }

        @EventHandler
        public void on(PlayerJoinEvent event) {
            event.setJoinMessage("joining " + event.getPlayer());
        }

        @EventHandler
        public void on(PlayerQuitEvent event) {
            event.setQuitMessage("quiting " + event.getPlayer());
        }

        @EventHandler
        public void on(BlockPlaceEvent event) {
            event.getPlayer().sendMessage("placing " + event.getBlockPlaced());
            if (event.getBlockPlaced().getType() == Material.TNT) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("cancelling tnt place");
            }
        }

        @EventHandler
        public void on(BlockMultiPlaceEvent event) {
            event.getPlayer().sendMessage("multi placing " + event.getReplacedBlockStates());
        }

        @EventHandler
        public void on(ChunkLoadEvent event) {
            //System.out.println("loading chunk " + event.getChunk().toString());
        }

        @EventHandler
        public void on(ChunkUnloadEvent event) {
             //System.out.println("unloading chunk " + event.getChunk().toString());
        }
    }
}
