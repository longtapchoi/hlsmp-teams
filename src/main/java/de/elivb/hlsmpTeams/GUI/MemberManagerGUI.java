package de.elivb.hlsmpTeams.GUI;

import de.elivb.hlsmpTeams.HexUtils;
import de.elivb.hlsmpTeams.Team;
import de.elivb.hlsmpTeams.Manager.DataManager;
import de.elivb.hlsmpTeams.Manager.TeamRankManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MemberManagerGUI implements Listener {
   private final Team plugin;
   private FileConfiguration guiConfig;
   private int pvpToggleSlot = 10;
   private int teamHomeAccessSlot = 11;
   private int teamHomeSetSlot = 12;
   private int deleteTeamSlot = 13;
   private int manageMembersSlot = 14;
   private int backSlot = 18;

   public MemberManagerGUI(Team plugin) {
      this.plugin = plugin;
      this.loadGUIConfig();
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   private void loadGUIConfig() {
      File guiFolder = new File(this.plugin.getDataFolder(), "gui");
      if (!guiFolder.exists()) {
         guiFolder.mkdirs();
      }

      File guiFile = new File(guiFolder, "member.manager.yml");
      if (!guiFile.exists()) {
         this.createDefaultConfig(guiFile);
      }

      this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
      this.loadConfigurableSlots();
   }

   private void loadConfigurableSlots() {
      if (this.guiConfig.contains("items.pvp_toggle.slot")) {
         this.pvpToggleSlot = this.guiConfig.getInt("items.pvp_toggle.slot");
      }

      if (this.guiConfig.contains("items.team_home_access.slot")) {
         this.teamHomeAccessSlot = this.guiConfig.getInt("items.team_home_access.slot");
      }

      if (this.guiConfig.contains("items.team_home_set.slot")) {
         this.teamHomeSetSlot = this.guiConfig.getInt("items.team_home_set.slot");
      }

      if (this.guiConfig.contains("items.delete_team.slot")) {
         this.deleteTeamSlot = this.guiConfig.getInt("items.delete_team.slot");
      }

      if (this.guiConfig.contains("items.manage_members.slot")) {
         this.manageMembersSlot = this.guiConfig.getInt("items.manage_members.slot");
      }

      if (this.guiConfig.contains("items.back.slot")) {
         this.backSlot = this.guiConfig.getInt("items.back.slot");
      }

   }

   private void createDefaultConfig(File guiFile) {
      try {
         FileConfiguration config = new YamlConfiguration();
         config.set("gui.title", "&8ᴍᴇᴍʙᴇʀ ᴍᴀɴᴀɢᴇʀ");
         config.set("gui.rows", 3);
         config.set("items.pvp_toggle.material", "DIAMOND_SWORD");
         config.set("items.pvp_toggle.slot", 10);
         config.set("items.pvp_toggle.title", "&#34eb9bᴛᴇᴀᴍ ᴘᴠᴘ");
         config.set("items.pvp_toggle.lore", Arrays.asList("&fStatus: %status%", "&7Click to toggle"));
         config.set("items.pvp_toggle.enabled_status", "&#02de4fAllowed");
         config.set("items.pvp_toggle.disabled_status", "&#e00202Denied");
         config.set("items.team_home_access.material", "LIGHT_GRAY_BED");
         config.set("items.team_home_access.slot", 11);
         config.set("items.team_home_access.title", "&#34eb9bᴛᴇᴀᴍ ʜᴏᴍᴇ");
         config.set("items.team_home_access.lore", Arrays.asList("&fAccess: %status%", "&7Click to toggle"));
         config.set("items.team_home_access.enabled_status", "&#02de4fAllowed");
         config.set("items.team_home_access.disabled_status", "&#e00202Denied");
         config.set("items.team_home_set.material", "RED_BED");
         config.set("items.team_home_set.slot", 12);
         config.set("items.team_home_set.title", "&#34eb9bꜱᴇᴛ ʜᴏᴍᴇ");
         config.set("items.team_home_set.lore", Arrays.asList("&fPermission: %status%", "&7Click to toggle"));
         config.set("items.team_home_set.enabled_status", "&#02de4fAllowed");
         config.set("items.team_home_set.disabled_status", "&#e00202Denied");
         config.set("items.delete_team.material", "BARRIER");
         config.set("items.delete_team.slot", 13);
         config.set("items.delete_team.title", "&#34eb9bᴛᴇᴀᴍ ᴅᴇʟᴇᴛᴇ");
         config.set("items.delete_team.lore", Arrays.asList("&fPermission: %status%", "&7Click to toggle"));
         config.set("items.delete_team.enabled_status", "&#02de4fAllowed");
         config.set("items.delete_team.disabled_status", "&#e00202Denied");
         config.set("items.manage_members.material", "PLAYER_HEAD");
         config.set("items.manage_members.slot", 14);
         config.set("items.manage_members.title", "&#34eb9bᴍᴀɴᴀɢᴇ ᴍᴇᴍʙᴇʀꜱ");
         config.set("items.manage_members.lore", Arrays.asList("&fPermission: %status%", "&7Click to toggle"));
         config.set("items.manage_members.enabled_status", "&#02de4fAllowed");
         config.set("items.manage_members.disabled_status", "&#e00202Denied");
         config.set("items.back.material", "ARROW");
         config.set("items.back.slot", 18);
         config.set("items.back.title", "&#34eb9bʙᴀᴄᴋ");
         config.set("items.back.lore", Arrays.asList("&fClick to go back"));
         config.save(guiFile);
      } catch (Exception var3) {
      }

   }

   public void openMemberManager(Player player, String targetPlayerName) {
      DataManager team = this.plugin.getTeamManager().getPlayerTeam(player.getName());
      if (team == null) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
         this.plugin.getSoundManager().playSound(player, "error-sound");
      } else if (!team.isMember(targetPlayerName)) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.player-not-in-your-team"));
         this.plugin.getSoundManager().playSound(player, "error-sound");
      } else {
         String title = HexUtils.colorize(this.guiConfig.getString("gui.title", "&8ᴍᴇᴍʙᴇʀ ᴍᴀɴᴀɢᴇʀ"));
         Inventory gui = Bukkit.createInventory(new MemberManagerHolder(targetPlayerName), 27, title);
         this.addConfigItem(gui, "pvp_toggle", player, targetPlayerName, team);
         this.addConfigItem(gui, "team_home_access", player, targetPlayerName, team);
         this.addConfigItem(gui, "team_home_set", player, targetPlayerName, team);
         this.addConfigItem(gui, "delete_team", player, targetPlayerName, team);
         this.addConfigItem(gui, "manage_members", player, targetPlayerName, team);
         this.addConfigItem(gui, "back", player, targetPlayerName, team);
         this.plugin.runTask(player, () -> player.openInventory(gui));
      }
   }

   private void addConfigItem(Inventory gui, String itemName, Player viewer, String targetPlayer, DataManager team) {
      String path = "items." + itemName;
      if (this.guiConfig.contains(path + ".material")) {
         Material material = Material.getMaterial(this.guiConfig.getString(path + ".material", "STONE"));
         if (material == null) {
            material = Material.STONE;
         }

         String displayName = HexUtils.colorize(this.guiConfig.getString(path + ".title", "").replace("%player%", targetPlayer));
         List<String> lore = new ArrayList();
         team.isLeader(targetPlayer);
         team.isLeader(viewer.getName());

         for(String line : this.guiConfig.getStringList(path + ".lore")) {
            String status = this.getStatusForItem(itemName, targetPlayer, team);
            String processed = line.replace("%player%", targetPlayer).replace("%status%", status);
            lore.add(HexUtils.colorize(processed));
         }

         int slot = this.guiConfig.getInt(path + ".slot", 0);
         ItemStack item = this.createItem(material, displayName, lore);
         gui.setItem(slot, item);
      }
   }

   private String getStatusForItem(String itemName, String targetPlayer, DataManager team) {
      TeamRankManager rankManager = this.plugin.getRankManager();
      boolean isOwner = team.isLeader(targetPlayer);
      if (isOwner) {
         return this.guiConfig.getString("items." + itemName + ".enabled_status", "&#02de4fAllowed");
      } else {
         switch (itemName) {
            case "pvp_toggle":
               boolean hasPvPPerm = rankManager.hasPermission(targetPlayer, team.getName(), "pvp_toggle");
               return hasPvPPerm ? this.guiConfig.getString("items.pvp_toggle.enabled_status", "&#02de4fAllowed") : this.guiConfig.getString("items.pvp_toggle.disabled_status", "&#e00202Denied");
            case "team_home_access":
               boolean homeAccess = rankManager.hasPermission(targetPlayer, team.getName(), "home_access");
               return homeAccess ? this.guiConfig.getString("items.team_home_access.enabled_status", "&#02de4fAllowed") : this.guiConfig.getString("items.team_home_access.disabled_status", "&#e00202Denied");
            case "team_home_set":
               boolean homeSet = rankManager.hasPermission(targetPlayer, team.getName(), "home_set");
               return homeSet ? this.guiConfig.getString("items.team_home_set.enabled_status", "&#02de4fAllowed") : this.guiConfig.getString("items.team_home_set.disabled_status", "&#e00202Denied");
            case "delete_team":
               boolean deletePerm = rankManager.hasPermission(targetPlayer, team.getName(), "delete_team");
               return deletePerm ? this.guiConfig.getString("items.delete_team.enabled_status", "&#02de4fAllowed") : this.guiConfig.getString("items.delete_team.disabled_status", "&#e00202Denied");
            case "manage_members":
               boolean managePerm = rankManager.hasPermission(targetPlayer, team.getName(), "manage_members");
               return managePerm ? this.guiConfig.getString("items.manage_members.enabled_status", "&#02de4fAllowed") : this.guiConfig.getString("items.manage_members.disabled_status", "&#e00202Denied");
            default:
               return "&#e00202Denied";
         }
      }
   }

   private boolean canEdit(Player viewer, String targetPlayer, DataManager team, String itemName) {
      if (team.isLeader(viewer.getName())) {
         return true;
      } else if (viewer.getName().equals(targetPlayer)) {
         return false;
      } else {
         TeamRankManager rankManager = this.plugin.getRankManager();
         switch (itemName) {
            case "pvp_toggle":
               return rankManager.hasPermission(viewer.getName(), team.getName(), "pvp_toggle");
            case "team_home_access":
            case "team_home_set":
               return rankManager.hasPermission(viewer.getName(), team.getName(), "home_set");
            case "delete_team":
               return rankManager.hasPermission(viewer.getName(), team.getName(), "delete_team");
            case "manage_members":
               return rankManager.hasPermission(viewer.getName(), team.getName(), "manage_members");
            default:
               return false;
         }
      }
   }

   private ItemStack createItem(Material material, String displayName, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(displayName);
         if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         if (event.getView().getTopInventory().getHolder() instanceof MemberManagerHolder) {
            event.setCancelled(true);
            Player player = (Player)event.getWhoClicked();
            MemberManagerHolder holder = (MemberManagerHolder)event.getView().getTopInventory().getHolder();
            String targetPlayer = holder.getTargetPlayer();
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
               DataManager team = this.plugin.getTeamManager().getPlayerTeam(player.getName());
               if (team == null) {
                  player.closeInventory();
                  player.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
                  this.plugin.getSoundManager().playSound(player, "error-sound");
               } else {
                  int slot = event.getSlot();
                  if (slot == this.backSlot) {
                     this.plugin.getSoundManager().playSound(player, "click-sound");
                     this.plugin.getTeamGUI().openTeamGUI(player);
                  } else {
                     String clickedItem = this.getItemForSlot(slot);
                     if (clickedItem != null) {
                        if (team.isLeader(targetPlayer) && !team.isLeader(player.getName())) {
                           player.sendMessage(this.plugin.getLangManager().getMessage("errors.cannot-edit-owner"));
                           this.plugin.getSoundManager().playSound(player, "error-sound");
                        } else if (!this.canEdit(player, targetPlayer, team, clickedItem)) {
                           player.sendMessage(this.plugin.getLangManager().getMessage("errors.insufficient-rank"));
                           this.plugin.getSoundManager().playSound(player, "error-sound");
                        } else {
                           this.handleItemClick(player, clickedItem, targetPlayer, team);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private String getItemForSlot(int slot) {
      if (slot == this.pvpToggleSlot) {
         return "pvp_toggle";
      } else if (slot == this.teamHomeAccessSlot) {
         return "team_home_access";
      } else if (slot == this.teamHomeSetSlot) {
         return "team_home_set";
      } else if (slot == this.deleteTeamSlot) {
         return "delete_team";
      } else {
         return slot == this.manageMembersSlot ? "manage_members" : null;
      }
   }

   private void handleItemClick(Player player, String itemName, String targetPlayer, DataManager team) {
      this.plugin.getSoundManager().playSound(player, "click-sound");
      TeamRankManager rankManager = this.plugin.getRankManager();
      switch (itemName) {
         case "pvp_toggle":
            boolean currentPvPPerm = rankManager.hasPermission(targetPlayer, team.getName(), "pvp_toggle");
            rankManager.setPermission(targetPlayer, team.getName(), "pvp_toggle", !currentPvPPerm);
            break;
         case "team_home_access":
            boolean currentAccess = rankManager.hasPermission(targetPlayer, team.getName(), "home_access");
            rankManager.setPermission(targetPlayer, team.getName(), "home_access", !currentAccess);
            break;
         case "team_home_set":
            boolean currentSetPerm = rankManager.hasPermission(targetPlayer, team.getName(), "home_set");
            rankManager.setPermission(targetPlayer, team.getName(), "home_set", !currentSetPerm);
            break;
         case "delete_team":
            boolean currentDeletePerm = rankManager.hasPermission(targetPlayer, team.getName(), "delete_team");
            rankManager.setPermission(targetPlayer, team.getName(), "delete_team", !currentDeletePerm);
            break;
         case "manage_members":
            boolean currentManage = rankManager.hasPermission(targetPlayer, team.getName(), "manage_members");
            rankManager.setPermission(targetPlayer, team.getName(), "manage_members", !currentManage);
      }

      this.openMemberManager(player, targetPlayer);
   }

   public void reloadConfig() {
      this.loadGUIConfig();
   }

   private static class MemberManagerHolder implements InventoryHolder {
      private final String targetPlayer;

      public MemberManagerHolder(String targetPlayer) {
         this.targetPlayer = targetPlayer;
      }

      public String getTargetPlayer() {
         return this.targetPlayer;
      }

      public Inventory getInventory() {
         return null;
      }
   }
}
