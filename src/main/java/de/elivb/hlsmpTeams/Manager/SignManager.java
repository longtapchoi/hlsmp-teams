package de.elivb.hlsmpTeams.Manager;

import de.elivb.hlsmpTeams.HexUtils;
import de.elivb.hlsmpTeams.Team;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SignManager implements Listener {
   private final Team plugin;
   private final Map<UUID, SignSession> activeSessions;
   private final Map<UUID, Location> signLocations;

   public SignManager(Team plugin) {
      this.plugin = plugin;
      this.activeSessions = new HashMap<>();
      this.signLocations = new HashMap<>();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void openSignEditor(Player player, BiConsumer<Player, String> callback) {
      UUID uuid = player.getUniqueId();
      closeSession(player);

      SignSession session = new SignSession(callback);
      activeSessions.put(uuid, session);

      // Đặt sign tại Y-5 dưới chân player
      Location playerLoc = player.getLocation();
      Location signLoc = playerLoc.getBlock().getLocation().subtract(0, 5, 0);
      Block block = signLoc.getBlock();

      session.oldBlockType = block.getType();
      session.oldBlockData = block.getBlockData();

      block.setType(Material.PALE_OAK_WALL_SIGN);
      signLocations.put(uuid, signLoc);

      // Set nội dung sign: line 0 = "> TÌM KIẾM <", line 1 = ">>"
      String[] lines = {"> TÌM KIẾM <", ">>", "", ""};

      BlockState state = block.getState();
      if (state instanceof Sign sign) {
         sign.setLine(0, lines[0]);
         sign.setLine(1, lines[1]);
         sign.setLine(2, lines[2]);
         sign.setLine(3, lines[3]);
         sign.update();
      }

      if (plugin.isFolia()) {
         player.sendSignChange(signLoc, lines);
         player.getScheduler().runDelayed(plugin, (scheduledTask) -> {
            if (player.isOnline()) {
               Location loc = signLocations.get(uuid);
               if (loc != null && loc.getWorld() != null) {
                  BlockState s = loc.getBlock().getState();
                  if (s instanceof Sign sign) {
                     try { player.openSign(sign); } catch (Exception ignored) {}
                  }
               }
            }
         }, null, 2L);
      } else {
         player.sendSignChange(signLoc, lines);
         plugin.runTaskLater(() -> {
            if (player.isOnline()) {
               Location loc = signLocations.get(uuid);
               if (loc != null && loc.getWorld() != null) {
                  BlockState s = loc.getBlock().getState();
                  if (s instanceof Sign sign) {
                     try { player.openSign(sign); } catch (Exception ignored) {}
                  }
               }
            }
         }, 2L);
      }
   }

   public void closeSession(Player player) {
      UUID uuid = player.getUniqueId();
      SignSession session = activeSessions.remove(uuid);
      if (session != null) {
         Location loc = signLocations.remove(uuid);
         if (loc != null && loc.getWorld() != null) {
            Block block = loc.getBlock();
            if (block.getType() == Material.PALE_OAK_WALL_SIGN) {
               if (session.oldBlockType != null && session.oldBlockType != Material.AIR) {
                  block.setType(session.oldBlockType);
                  if (session.oldBlockData != null) block.setBlockData(session.oldBlockData);
               } else {
                  block.setType(Material.AIR);
               }
            }
         }
      }
   }

   @EventHandler
   public void onSignChange(SignChangeEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (!activeSessions.containsKey(uuid)) return;

      SignSession session = activeSessions.get(uuid);
      event.setCancelled(true);

      // Đọc line(1) — chỗ player nhập
      String input = event.getLine(1);
      if (input == null) input = "";
      input = input.trim();

      // Bỏ qua nếu player không thay đổi (vẫn là ">>")
      if (input.equals(">>")) input = "";

      activeSessions.remove(uuid);
      Location loc = signLocations.remove(uuid);
      if (loc != null && loc.getWorld() != null) {
         Block block = loc.getBlock();
         if (block.getType() == Material.PALE_OAK_WALL_SIGN) {
            if (session.oldBlockType != null && session.oldBlockType != Material.AIR) {
               block.setType(session.oldBlockType);
               if (session.oldBlockData != null) block.setBlockData(session.oldBlockData);
            } else {
               block.setType(Material.AIR);
            }
         }
      }

      if (!input.isEmpty() && session.callback != null) {
         final String finalInput = input;
         session.callback.accept(player, finalInput);
      }
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (!activeSessions.containsKey(uuid)) return;
      if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)
            && event.getClickedBlock() != null
            && event.getClickedBlock().getType() == Material.PALE_OAK_WALL_SIGN) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (!activeSessions.containsKey(uuid)) return;
      Location signLoc = signLocations.get(uuid);
      if (signLoc != null && event.getBlock().getLocation().equals(signLoc)) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      closeSession(event.getPlayer());
   }

   private static class SignSession {
      final BiConsumer<Player, String> callback;
      Material oldBlockType;
      BlockData oldBlockData;

      SignSession(BiConsumer<Player, String> callback) {
         this.callback = callback;
         this.oldBlockType = Material.AIR;
      }
   }
}
