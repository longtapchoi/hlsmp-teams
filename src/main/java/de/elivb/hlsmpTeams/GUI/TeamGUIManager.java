package de.elivb.hlsmpTeams.GUI;

import com.mojang.authlib.GameProfile;
import de.elivb.hlsmpTeams.Team;
import de.elivb.hlsmpTeams.Manager.DataManager;
import de.elivb.hlsmpTeams.Manager.TeamRankManager;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class TeamGUIManager {
   private final Team plugin;
   private final TeamGUI teamGUI;
   private final Map<UUID, PlayerSettings> playerSettings;
   private final Map<String, ItemStack> cachedPlayerHeads;
   private final Map<String, UUID> nameToUUIDCache;
   private final Map<UUID, Object> loadingTasks;
   private final Set<UUID> waitingForChatInput;

   public TeamGUIManager(Team plugin, TeamGUI teamGUI) {
      this.plugin = plugin;
      this.teamGUI = teamGUI;
      this.playerSettings = new ConcurrentHashMap();
      this.cachedPlayerHeads = new ConcurrentHashMap();
      this.nameToUUIDCache = new ConcurrentHashMap();
      this.loadingTasks = new ConcurrentHashMap();
      this.waitingForChatInput = ConcurrentHashMap.newKeySet();
   }

   public PlayerSettings getPlayerSettings(UUID playerUuid) {
      return (PlayerSettings)this.playerSettings.computeIfAbsent(playerUuid, (k) -> new PlayerSettings());
   }

   public void setSearchQuery(UUID playerUuid, String query) {
      this.getPlayerSettings(playerUuid).setSearchQuery(query);
   }

   public void setWaitingForChatInput(UUID playerUuid, boolean waiting) {
      if (waiting) {
         this.waitingForChatInput.add(playerUuid);
      } else {
         this.waitingForChatInput.remove(playerUuid);
      }

   }

   public boolean isWaitingForChatInput(UUID playerUuid) {
      return this.waitingForChatInput.contains(playerUuid);
   }

   public void resetPlayerSettings(Player player) {
      UUID uuid = player.getUniqueId();
      this.playerSettings.remove(uuid);
      this.waitingForChatInput.remove(uuid);
      Object task = this.loadingTasks.remove(uuid);
      if (task instanceof BukkitRunnable) {
         ((BukkitRunnable)task).cancel();
      }

   }

   public boolean isHeadCached(String playerName) {
      return this.cachedPlayerHeads.containsKey(playerName);
   }

   public ItemStack getCachedPlayerHead(String playerName, String teamName) {
      ItemStack head = (ItemStack)this.cachedPlayerHeads.get(playerName);
      if (head != null) {
         head = head.clone();
         return head;
      } else {
         return this.createPlayerHead(playerName, teamName);
      }
   }

   public void clearHeadCache() {
      this.cachedPlayerHeads.clear();
      this.nameToUUIDCache.clear();
      this.loadingTasks.values().forEach((task) -> {
         if (task instanceof BukkitRunnable) {
            ((BukkitRunnable)task).cancel();
         }

      });
      this.loadingTasks.clear();
   }

   public void removeFromHeadCache(String playerName) {
      this.cachedPlayerHeads.remove(playerName);
      this.nameToUUIDCache.remove(playerName);
   }

   public void loadMissingHeadsAsync(final Player player, final DataManager team, List<String> allMembers, final List<String> missingHeads, final int page) {
      final UUID playerUUID = player.getUniqueId();
      if (this.loadingTasks.containsKey(playerUUID)) {
         Object oldTask = this.loadingTasks.get(playerUUID);
         if (oldTask instanceof BukkitRunnable) {
            ((BukkitRunnable)oldTask).cancel();
         }

         this.loadingTasks.remove(playerUUID);
      }

      final AtomicBoolean isRunning = new AtomicBoolean(true);
      final Runnable loadRunnable = new Runnable() {
         private int currentIndex = 0;

         public void run() {
            if (isRunning.get()) {
               if (this.currentIndex >= missingHeads.size()) {
                  TeamGUIManager.this.plugin.runTask(player, () -> TeamGUIManager.this.teamGUI.openTeamGUI(player, page));
                  isRunning.set(false);
                  TeamGUIManager.this.loadingTasks.remove(playerUUID);
               } else {
                  String memberName = (String)missingHeads.get(this.currentIndex);
                  TeamGUIManager.this.plugin.runTaskAsynchronously(() -> {
                     ItemStack head = TeamGUIManager.this.createPlayerHead(memberName, team.getName());
                     TeamGUIManager.this.plugin.runTask(() -> {
                        if (isRunning.get() && !TeamGUIManager.this.cachedPlayerHeads.containsKey(memberName)) {
                           TeamGUIManager.this.cachedPlayerHeads.put(memberName, head);
                        }

                     });
                  });
                  ++this.currentIndex;
               }
            }
         }
      };
      if (this.plugin.isFolia()) {
         this.plugin.runTaskTimer(() -> loadRunnable.run(), 1L, 2L);
      } else {
         BukkitRunnable bukkitTask = new BukkitRunnable() {
            public void run() {
               loadRunnable.run();
            }
         };
         bukkitTask.runTaskTimer(this.plugin, 0L, 2L);
         this.loadingTasks.put(playerUUID, bukkitTask);
      }

      this.teamGUI.openPlaceholderGUI(player, team, allMembers, page);
   }

   public ItemStack createPlayerHead(String playerName, String teamName) {
      if (this.cachedPlayerHeads.containsKey(playerName)) {
         return ((ItemStack)this.cachedPlayerHeads.get(playerName)).clone();
      } else {
         new ItemStack(Material.PLAYER_HEAD);
         Player onlinePlayer = Bukkit.getPlayer(playerName);
         if (onlinePlayer != null) {
            ItemStack head = this.getHeadFromOnlinePlayer(onlinePlayer);
            if (head != null) {
               return head;
            }
         }

         OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
         if (offlinePlayer.hasPlayedBefore()) {
            ItemStack var7 = this.getHeadFromOfflinePlayer(offlinePlayer);
            if (var7 != null) {
               return var7;
            }
         }

         UUID uuid = this.getUUIDFromName(playerName);
         if (uuid != null) {
            ItemStack var8 = this.getHeadFromUUID(uuid, playerName);
            if (var8 != null) {
               return var8;
            }
         }

         return new ItemStack(Material.PLAYER_HEAD);
      }
   }

   private ItemStack getHeadFromOnlinePlayer(Player player) {
      try {
         ItemStack head = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta meta = (SkullMeta)head.getItemMeta();
         if (meta != null) {
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
            return head;
         }
      } catch (Exception var4) {
      }

      return null;
   }

   private ItemStack getHeadFromOfflinePlayer(OfflinePlayer offlinePlayer) {
      try {
         ItemStack head = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta meta = (SkullMeta)head.getItemMeta();
         if (meta != null) {
            meta.setOwningPlayer(offlinePlayer);
            head.setItemMeta(meta);
            return head;
         }
      } catch (Exception var4) {
      }

      return null;
   }

   private UUID getUUIDFromName(String playerName) {
      if (this.nameToUUIDCache.containsKey(playerName)) {
         return (UUID)this.nameToUUIDCache.get(playerName);
      } else {
         OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
         if (offlinePlayer.hasPlayedBefore()) {
            UUID uuid = offlinePlayer.getUniqueId();
            if (uuid != null && uuid.version() != 0) {
               this.nameToUUIDCache.put(playerName, uuid);
               return uuid;
            }
         }

         try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            Scanner scanner = new Scanner(url.openStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();
            if (response.contains("\"id\"")) {
               String uuidString = response.split("\"id\":\"")[1].split("\"")[0];
               UUID uuid = this.formatUUID(uuidString);
               if (uuid != null) {
                  this.nameToUUIDCache.put(playerName, uuid);
                  return uuid;
               }
            }
         } catch (Exception var8) {
         }

         return null;
      }
   }

   private ItemStack getHeadFromUUID(UUID uuid, String playerName) {
      try {
         ItemStack head = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta meta = (SkullMeta)head.getItemMeta();
         if (meta != null) {
            GameProfile profile = new GameProfile(uuid, playerName);
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
            head.setItemMeta(meta);
            return head;
         }
      } catch (Exception var7) {
      }

      return null;
   }

   private UUID formatUUID(String uuidString) {
      try {
         if (uuidString.length() == 32) {
            String var10000 = uuidString.substring(0, 8);
            String formatted = var10000 + "-" + uuidString.substring(8, 12) + "-" + uuidString.substring(12, 16) + "-" + uuidString.substring(16, 20) + "-" + uuidString.substring(20, 32);
            return UUID.fromString(formatted);
         }

         if (uuidString.length() == 36) {
            return UUID.fromString(uuidString);
         }
      } catch (Exception var3) {
      }

      return null;
   }

   public void sortMembers(List<String> members, SortOption option, String teamName) {
      DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
      switch (option.ordinal()) {
         case 0:
            if (team != null) {
               members.sort((m1, m2) -> {
                  long t1 = team.getMemberJoinTime(m1);
                  long t2 = team.getMemberJoinTime(m2);
                  if (t1 == 0L) {
                     t1 = Long.MAX_VALUE;
                  }

                  if (t2 == 0L) {
                     t2 = Long.MAX_VALUE;
                  }

                  return Long.compare(t1, t2);
               });
            } else {
               members.sort(String.CASE_INSENSITIVE_ORDER);
            }
            break;
         case 1:
            members.sort((m1, m2) -> {
               DataManager teamData = this.plugin.getTeamManager().getTeamByName(teamName);
               if (teamData == null) {
                  return m1.compareToIgnoreCase(m2);
               } else {
                  boolean isOwner1 = teamData.isLeader(m1);
                  boolean isOwner2 = teamData.isLeader(m2);
                  if (isOwner1 && !isOwner2) {
                     return -1;
                  } else if (!isOwner1 && isOwner2) {
                     return 1;
                  } else {
                     int permCount1 = this.countPermissions(m1, teamName);
                     int permCount2 = this.countPermissions(m2, teamName);
                     return permCount1 != permCount2 ? Integer.compare(permCount2, permCount1) : m1.compareToIgnoreCase(m2);
                  }
               }
            });
            break;
         case 2:
            members.sort(String.CASE_INSENSITIVE_ORDER);
            break;
         case 3:
            members.sort((m1, m2) -> {
               boolean online1 = Bukkit.getPlayer(m1) != null;
               boolean online2 = Bukkit.getPlayer(m2) != null;
               if (online1 && !online2) {
                  return -1;
               } else {
                  return !online1 && online2 ? 1 : m1.compareToIgnoreCase(m2);
               }
            });
      }

   }

   private int countPermissions(String playerName, String teamName) {
      DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
      if (team == null) {
         return 0;
      } else if (team.isLeader(playerName)) {
         return 100;
      } else {
         int count = 0;
         TeamRankManager rankManager = this.plugin.getRankManager();
         if (rankManager.hasPermission(playerName, teamName, "pvp_toggle")) {
            ++count;
         }

         if (rankManager.hasPermission(playerName, teamName, "home_access")) {
            ++count;
         }

         if (rankManager.hasPermission(playerName, teamName, "home_set")) {
            ++count;
         }

         if (rankManager.hasPermission(playerName, teamName, "manage_members")) {
            ++count;
         }

         if (rankManager.hasPermission(playerName, teamName, "delete_team")) {
            ++count;
         }

         return count;
      }
   }

   public static class PlayerSettings {
      private int currentPage = 0;
      private String searchQuery = "";
      private SortOption sortOption;

      public PlayerSettings() {
         this.sortOption = TeamGUIManager.SortOption.ONLINE_MEMBERS;
      }

      public int getCurrentPage() {
         return this.currentPage;
      }

      public void setCurrentPage(int page) {
         this.currentPage = page;
      }

      public String getSearchQuery() {
         return this.searchQuery;
      }

      public void setSearchQuery(String query) {
         this.searchQuery = query;
      }

      public SortOption getSortOption() {
         return this.sortOption;
      }

      public void setSortOption(SortOption option) {
         this.sortOption = option;
      }
   }

   public static enum SortOption {
      JOIN_DATE("Join Date"),
      PERMISSIONS("Permissions"),
      ALPHABETICALLY("Alphabetically"),
      ONLINE_MEMBERS("Online Members");

      private final String displayName;

      private SortOption(String displayName) {
         this.displayName = displayName;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      // $FF: synthetic method
      private static SortOption[] $values() {
         return new SortOption[]{JOIN_DATE, PERMISSIONS, ALPHABETICALLY, ONLINE_MEMBERS};
      }
   }
}
