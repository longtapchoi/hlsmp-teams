package de.elivb.hlsmpTeams.Manager;

import de.elivb.hlsmpTeams.HexUtils;
import de.elivb.hlsmpTeams.Team;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HomeManager {
   private final Team plugin;
   private final Map<UUID, Object[]> activeTeleports;
   private final Map<UUID, Boolean> teleportingPlayers;
   private int teleportDelay = 5;
   private double maxDistance = 0.3;

   public HomeManager(Team plugin) {
      this.plugin = plugin;
      this.activeTeleports = new HashMap();
      this.teleportingPlayers = new HashMap();
      this.loadTeleportSettings();
   }

   private void loadTeleportSettings() {
      this.teleportDelay = this.plugin.getConfig().getInt("team-home.teleport-delay", 5);
      this.maxDistance = this.plugin.getConfig().getDouble("team-home.teleport-distance", 0.3);
   }

   public void reloadSettings() {
      this.loadTeleportSettings();
   }

   private boolean isHomeEnabled() {
      return this.plugin.getConfig().getBoolean("team-home.enabled", true);
   }

   private void sendActionBar(Player player, String message) {
      String coloredMessage = HexUtils.colorize(message);
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(coloredMessage));
   }

   public boolean isTeleporting(Player player) {
      return (Boolean)this.teleportingPlayers.getOrDefault(player.getUniqueId(), false);
   }

   private void setTeleporting(Player player, boolean teleporting) {
      if (teleporting) {
         this.teleportingPlayers.put(player.getUniqueId(), true);
      } else {
         this.teleportingPlayers.remove(player.getUniqueId());
      }

   }

   private void cancelTeleport(Player player) {
      Object[] taskData = this.activeTeleports.remove(player.getUniqueId());
      if (taskData != null && taskData[0] != null) {
         this.plugin.cancelTask(taskData[0]);
      }

      this.setTeleporting(player, false);
   }

   public boolean setHome(Player player) {
      if (!this.isHomeEnabled()) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.team-home-disabled"));
         this.plugin.getSoundManager().playSound(player, "error-sound");
         return false;
      } else {
         DataManager team = this.plugin.getTeamManager().getPlayerTeam(player.getName());
         if (team == null) {
            player.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
            this.plugin.getSoundManager().playSound(player, "error-sound");
            return false;
         } else {
            if (!team.isLeader(player.getName())) {
               boolean hasPerm = this.plugin.getRankManager().hasPermission(player.getName(), team.getName(), "home_set");
               if (!hasPerm) {
                  player.sendMessage(this.plugin.getLangManager().getMessage("errors.insufficient-rank"));
                  this.plugin.getSoundManager().playSound(player, "error-sound");
                  return false;
               }
            }

            Location homeLocation = player.getLocation();
            team.setHome(homeLocation);
            player.sendMessage(this.plugin.getLangManager().getMessage("team.home-set").replace("%x%", String.valueOf(homeLocation.getBlockX())).replace("%y%", String.valueOf(homeLocation.getBlockY())).replace("%z%", String.valueOf(homeLocation.getBlockZ())));
            this.plugin.getSoundManager().playSound(player, "success-sound");
            return true;
         }
      }
   }

   public boolean teleportToHome(Player player) {
      if (!this.isHomeEnabled()) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.team-home-disabled"));
         this.plugin.getSoundManager().playSound(player, "error-sound");
         return false;
      } else {
         DataManager team = this.plugin.getTeamManager().getPlayerTeam(player.getName());
         if (team == null) {
            player.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
            this.plugin.getSoundManager().playSound(player, "error-sound");
            return false;
         } else {
            if (!team.isLeader(player.getName())) {
               boolean hasPerm = this.plugin.getRankManager().hasPermission(player.getName(), team.getName(), "home_access");
               if (!hasPerm) {
                  player.sendMessage(this.plugin.getLangManager().getMessage("errors.insufficient-rank"));
                  this.plugin.getSoundManager().playSound(player, "error-sound");
                  return false;
               }
            }

            Location homeLocation = team.getHome();
            if (homeLocation == null) {
               player.sendMessage(this.plugin.getLangManager().getMessage("errors.no-team-home"));
               this.plugin.getSoundManager().playSound(player, "error-sound");
               return false;
            } else if (homeLocation.getWorld() == null) {
               return false;
            } else if (this.isTeleporting(player)) {
               this.sendActionBar(player, this.plugin.getLangManager().getMessageWithoutPrefix("team.already-teleporting"));
               this.plugin.getSoundManager().playSound(player, "error-sound");
               return false;
            } else {
               if (player.hasPermission("team.home.bypass")) {
                  this.performTeleport(player, homeLocation);
               } else {
                  this.startTeleport(player, homeLocation);
               }

               return true;
            }
         }
      }
   }

   private void performTeleport(Player player, Location location) {
      if (this.plugin.isFolia()) {
         player.teleportAsync(location).thenAccept((success) -> {
            if (success) {
               this.plugin.runTask(player, () -> {
                  this.sendActionBar(player, this.plugin.getLangManager().getMessageWithoutPrefix("team.teleport-completed"));
                  this.plugin.getSoundManager().playSound(player, "teleport-complete");
               });
            }

         });
      } else {
         player.teleport(location);
         this.sendActionBar(player, this.plugin.getLangManager().getMessageWithoutPrefix("team.teleport-completed"));
         this.plugin.getSoundManager().playSound(player, "teleport-complete");
      }

   }

   private void startTeleport(final Player player, final Location targetLocation) {
      this.setTeleporting(player, true);
      final Location startLocation = player.getLocation().clone();
      final String startWorldName = startLocation.getWorld().getName();
      final double maxDistanceSquared = this.maxDistance * this.maxDistance;
      final int teleportDelayCopy = this.teleportDelay;
      final Object[] taskHolder = new Object[1];
      String startMessage = this.plugin.getLangManager().getMessageWithoutPrefix("team.teleport-countdown").replace("%count%", String.valueOf(teleportDelayCopy));
      this.sendActionBar(player, startMessage);
      this.plugin.getSoundManager().playSound(player, "teleport-cooldown");
      Runnable countdownTask = new Runnable() {
         int count = teleportDelayCopy;
         boolean firstRun = true;
         Location lastValidLocation = startLocation.clone();
         String lastWorldName = startWorldName;
         boolean isCancelled = false;

         private void cancelTask() {
            if (!this.isCancelled) {
               this.isCancelled = true;
               if (taskHolder[0] != null) {
                  HomeManager.this.plugin.cancelTask(taskHolder[0]);
                  taskHolder[0] = null;
               }

               HomeManager.this.setTeleporting(player, false);
               HomeManager.this.activeTeleports.remove(player.getUniqueId());
            }
         }

         public void run() {
            if (!this.isCancelled) {
               if (this.firstRun) {
                  this.firstRun = false;
               } else if (HomeManager.this.isTeleporting(player) && player.isOnline()) {
                  DataManager team = HomeManager.this.plugin.getTeamManager().getPlayerTeam(player.getName());
                  if (team != null && team.getHome() != null) {
                     Location currentLocation = player.getLocation();
                     String currentWorldName = currentLocation.getWorld().getName();
                     if (!currentWorldName.equals(this.lastWorldName)) {
                        this.lastWorldName = currentWorldName;
                        this.lastValidLocation = currentLocation.clone();
                     } else {
                        double deltaX = currentLocation.getX() - this.lastValidLocation.getX();
                        double deltaY = currentLocation.getY() - this.lastValidLocation.getY();
                        double deltaZ = currentLocation.getZ() - this.lastValidLocation.getZ();
                        double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                        if (distanceSquared > maxDistanceSquared) {
                           HomeManager.this.plugin.runTask(player, () -> {
                              HomeManager.this.sendActionBar(player, HomeManager.this.plugin.getLangManager().getMessageWithoutPrefix("team.teleport-cancelled"));
                              HomeManager.this.plugin.getSoundManager().playSound(player, "teleport-cancel");
                           });
                           this.cancelTask();
                           return;
                        }

                        this.lastValidLocation = currentLocation.clone();
                     }

                     --this.count;
                     if (this.count <= 0) {
                        HomeManager.this.plugin.runTask(player, () -> HomeManager.this.performTeleport(player, targetLocation));
                        this.cancelTask();
                     } else {
                        int currentCount = this.count;
                        HomeManager.this.plugin.runTask(player, () -> {
                           String countdownMessage = HomeManager.this.plugin.getLangManager().getMessageWithoutPrefix("team.teleport-countdown").replace("%count%", String.valueOf(currentCount));
                           HomeManager.this.sendActionBar(player, countdownMessage);
                           HomeManager.this.plugin.getSoundManager().playSound(player, "teleport-cooldown");
                        });
                     }

                  } else {
                     HomeManager.this.plugin.runTask(player, () -> {
                        player.sendMessage(HomeManager.this.plugin.getLangManager().getMessage("errors.no-team-home"));
                        HomeManager.this.plugin.getSoundManager().playSound(player, "error-sound");
                     });
                     this.cancelTask();
                  }
               } else {
                  this.cancelTask();
               }
            }
         }
      };
      taskHolder[0] = this.plugin.runGlobalTimer(countdownTask, 1L, 20L);
      this.activeTeleports.put(player.getUniqueId(), taskHolder);
   }

   public boolean deleteHome(Player player) {
      if (!this.isHomeEnabled()) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.team-home-disabled"));
         this.plugin.getSoundManager().playSound(player, "error-sound");
         return false;
      } else {
         DataManager team = this.plugin.getTeamManager().getPlayerTeam(player.getName());
         if (team == null) {
            player.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
            this.plugin.getSoundManager().playSound(player, "error-sound");
            return false;
         } else if (!team.isLeader(player.getName())) {
            player.sendMessage(this.plugin.getLangManager().getMessage("errors.insufficient-rank"));
            this.plugin.getSoundManager().playSound(player, "error-sound");
            return false;
         } else if (!team.hasHome()) {
            player.sendMessage(this.plugin.getLangManager().getMessage("errors.no-team-home"));
            this.plugin.getSoundManager().playSound(player, "error-sound");
            return false;
         } else {
            team.deleteHome();
            player.sendMessage(this.plugin.getLangManager().getMessage("team.home-deleted"));
            this.plugin.getSoundManager().playSound(player, "success-sound");
            return true;
         }
      }
   }

   public Location getTeamHome(String teamName) {
      if (!this.isHomeEnabled()) {
         return null;
      } else {
         DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
         return team != null ? team.getHome() : null;
      }
   }

   public boolean hasTeamHome(String teamName) {
      if (!this.isHomeEnabled()) {
         return false;
      } else {
         DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
         return team != null && team.hasHome();
      }
   }

   public void removeTeamHome(String teamName) {
      if (this.isHomeEnabled()) {
         DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
         if (team != null) {
            team.deleteHome();
         }

      }
   }

   public void setTeamHome(String teamName, Location location) {
      if (this.isHomeEnabled()) {
         DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
         if (team != null) {
            team.setHome(location);
         }

      }
   }

   public void onDisable() {
      this.cancelAllTeleports();
   }

   public void cancelAllTeleports() {
      for(Object[] taskData : this.activeTeleports.values()) {
         if (taskData != null && taskData[0] != null) {
            this.plugin.cancelTask(taskData[0]);
         }
      }

      this.activeTeleports.clear();
      this.teleportingPlayers.clear();
   }
}
