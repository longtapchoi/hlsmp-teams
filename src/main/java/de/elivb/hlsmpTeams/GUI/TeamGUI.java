package de.elivb.hlsmpTeams.GUI;

import de.elivb.hlsmpTeams.HexUtils;
import de.elivb.hlsmpTeams.Team;
import de.elivb.hlsmpTeams.Manager.DataManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TeamGUI implements Listener {
   private final Team plugin;
   private final TeamGUIManager guiManager;
   private FileConfiguration guiConfig;
   private List<TeamGUIManager.SortOption> configuredSortOrder = new ArrayList();
   private int searchSlot = 45;
   private int sortSlot = 46;
   private int backSlot = 48;
   private int teamInfoSlot = 49;
   private int nextSlot = 50;
   private int teamHomeSlot = 52;
   private int pvpSlot = 53;
   private static final int ITEMS_PER_PAGE = 27;
   private List<String> signGuiLines;

   public TeamGUI(Team plugin) {
      this.plugin = plugin;
      this.guiManager = new TeamGUIManager(plugin, this);
      this.loadGUIConfig();
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   private void loadGUIConfig() {
      File guiFolder = new File(this.plugin.getDataFolder(), "gui");
      if (!guiFolder.exists()) {
         guiFolder.mkdirs();
      }

      File guiFile = new File(guiFolder, "main.gui.yml");
      if (!guiFile.exists()) {
         this.createDefaultConfig(guiFile);
      }

      this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
      this.ensureSortOptionsExists(guiFile);
      this.signGuiLines = this.guiConfig.getStringList("sign-gui");
      if (this.signGuiLines == null || this.signGuiLines.size() < 4) {
         this.signGuiLines = Arrays.asList("", "↑↑↑↑↑↑↑↑↑↑↑↑↑", "   Search   ", "");
      }

      this.loadConfigurableSlots();
      this.configuredSortOrder.clear();
      if (this.guiConfig.contains("items.sort.options")) {
         ConfigurationSection section = this.guiConfig.getConfigurationSection("items.sort.options");
         if (section != null) {
            for(String key : section.getKeys(false)) {
               try {
                  this.configuredSortOrder.add(TeamGUIManager.SortOption.valueOf(key));
               } catch (IllegalArgumentException var7) {
               }
            }
         }
      }

      if (this.configuredSortOrder.isEmpty()) {
         this.configuredSortOrder.addAll(Arrays.asList(TeamGUIManager.SortOption.values()));
      }

   }

   private void loadConfigurableSlots() {
      if (this.guiConfig.contains("items.search.slot")) {
         this.searchSlot = this.guiConfig.getInt("items.search.slot");
      }

      if (this.guiConfig.contains("items.sort.slot")) {
         this.sortSlot = this.guiConfig.getInt("items.sort.slot");
      }

      if (this.guiConfig.contains("items.back.slot")) {
         this.backSlot = this.guiConfig.getInt("items.back.slot");
      }

      if (this.guiConfig.contains("items.team-info.slot")) {
         this.teamInfoSlot = this.guiConfig.getInt("items.team-info.slot");
      }

      if (this.guiConfig.contains("items.next.slot")) {
         this.nextSlot = this.guiConfig.getInt("items.next.slot");
      }

      if (this.guiConfig.contains("items.team-home.slot")) {
         this.teamHomeSlot = this.guiConfig.getInt("items.team-home.slot");
      }

      if (this.guiConfig.contains("items.pvp.slot")) {
         this.pvpSlot = this.guiConfig.getInt("items.pvp.slot");
      }

   }

   private void ensureSortOptionsExists(File guiFile) {
      try {
         boolean changed = false;
         if (this.guiConfig == null) {
            return;
         }

         if (this.guiConfig.contains("items.sort.options")) {
            ConfigurationSection section = this.guiConfig.getConfigurationSection("items.sort.options");

            for(TeamGUIManager.SortOption s : TeamGUIManager.SortOption.values()) {
               if (section == null || !section.contains(s.name())) {
                  this.guiConfig.set("items.sort.options." + s.name(), s.getDisplayName());
                  changed = true;
               }
            }
         } else {
            for(TeamGUIManager.SortOption s : TeamGUIManager.SortOption.values()) {
               this.guiConfig.set("items.sort.options." + s.name(), s.getDisplayName());
            }

            changed = true;
         }

         if (!this.guiConfig.contains("items.sort.slot")) {
            this.guiConfig.set("items.sort.slot", 46);
            changed = true;
         }

         if (changed) {
            this.guiConfig.save(guiFile);
         }
      } catch (Exception var8) {
      }

   }

   private void createDefaultConfig(File guiFile) {
      try {
         FileConfiguration config = new YamlConfiguration();
         config.set("chat-input.SignAPI", true);
         config.set("chat-input.ChatAPI", false);
         config.set("sign-gui", Arrays.asList("", "↑↑↑↑↑↑↑↑↑↑↑↑↑", "   Search   ", ""));
         config.set("gui.title", "&8ᴛᴇᴀᴍ (Page %current_page%)");
         config.set("gui.rows", 6);
         config.set("background.fill", false);
         config.set("background.material", "BLACK_STAINED_GLASS_PANE");
         config.set("background.name", "");
         config.set("background.lore", new ArrayList());
         config.set("items.search.material", "OAK_SIGN");
         config.set("items.search.name", "&#34eb9bꜱᴇᴀʀᴄʜ");
         config.set("items.search.lore", Collections.singletonList("&fClick to search"));
         config.set("items.search.slot", 45);
         config.set("items.search.action", "SEARCH");
         config.set("items.sort.material", "HOPPER");
         config.set("items.sort.name", "&#34eb9bѕᴏʀᴛ");
         config.set("items.sort.selected-prefix", "&#34eb9b• ");
         config.set("items.sort.unselected-prefix", "&f• ");
         config.set("items.sort.slot", 46);
         config.set("items.sort.action", "SORT");
         Map<String, String> sortOptions = new HashMap();
         sortOptions.put("JOIN_DATE", "Join Date");
         sortOptions.put("PERMISSIONS", "Permissions");
         sortOptions.put("ALPHABETICALLY", "Alphabetically");
         sortOptions.put("ONLINE_MEMBERS", "Online Members");
         config.set("items.sort.options", sortOptions);
         config.set("items.back.material", "ARROW");
         config.set("items.back.display-name", "&#34eb9bʙᴀᴄᴋ");
         config.set("items.back.lore", Collections.singletonList("&fClick to go to previous page"));
         config.set("items.back.slot", 48);
         config.set("items.back.action", "BACK");
         config.set("items.team-info.material", "IRON_HELMET");
         config.set("items.team-info.display-name", "&#34eb9bᴛᴇᴀᴍ %team%");
         config.set("items.team-info.lore", Arrays.asList("&fClick to refresh", "&fAdd up to 27 team members"));
         config.set("items.team-info.slot", 49);
         config.set("items.team-info.action", "REFRESH");
         config.set("items.next.material", "ARROW");
         config.set("items.next.display-name", "&#34eb9bɴᴇxᴛ");
         config.set("items.next.lore", Collections.singletonList("&fClick to go to next page"));
         config.set("items.next.slot", 50);
         config.set("items.next.action", "NEXT");
         config.set("items.team-home.material", "WHITE_BANNER");
         config.set("items.team-home.display-name", "&#34eb9bᴛᴇᴀᴍ ʜᴏᴍᴇ");
         config.set("items.team-home.lore", Collections.singletonList("&fSet the team home with /team sethome"));
         config.set("items.team-home.slot", 52);
         config.set("items.team-home.action", "HOME");
         config.set("items.pvp.material", "DIAMOND_SWORD");
         config.set("items.pvp.display-name", "&#34eb9bᴘᴠᴘ");
         config.set("items.pvp.lore", Collections.singletonList("&fCurrently: %pvp-status%"));
         config.set("items.pvp.status.enabled", "&#34eb9b&lON");
         config.set("items.pvp.status.disabled", "&#FF0000&lOFF");
         config.set("items.pvp.slot", 53);
         config.set("items.pvp.action", "PVP");
         config.set("items.source-item.material", "PLAYER_HEAD");
         config.set("items.source-item.display-name", "&#34eb9b%player%");
         List<String> sourceLore = Arrays.asList("&fClick to manage");
         config.set("items.source-item.lore", sourceLore);
         config.save(guiFile);
      } catch (Exception var5) {
      }

   }

   private boolean isTeamHomeVisibleInGUI() {
      return this.plugin.getConfig().getBoolean("team-home.enabled", true);
   }

   public void openTeamGUI(Player player) {
      this.openTeamGUI(player, 0);
   }

   public void openTeamGUI(Player player, int page) {
      DataManager team = this.plugin.getTeamManager().getPlayerTeam(player.getName());
      if (team == null) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
      } else {
         TeamGUIManager.PlayerSettings settings = this.guiManager.getPlayerSettings(player.getUniqueId());
         TeamGUIManager.SortOption sortOption = settings.getSortOption();
         List<String> members = new ArrayList(team.getMembers());
         String searchQuery = settings.getSearchQuery();
         if (!searchQuery.isEmpty()) {
            members.removeIf((memberx) -> !memberx.toLowerCase().contains(searchQuery.toLowerCase()));
         }

         this.guiManager.sortMembers(members, sortOption, team.getName());
         int totalPages = Math.max(1, (int)Math.ceil((double)members.size() / (double)27.0F));
         if (page >= totalPages) {
            page = totalPages - 1;
         }

         if (page < 0) {
            page = 0;
         }

         settings.setCurrentPage(page);
         List<String> missingHeads = new ArrayList();

         for(String member : members) {
            if (!this.guiManager.isHeadCached(member)) {
               missingHeads.add(member);
            }
         }

         if (!missingHeads.isEmpty()) {
            this.guiManager.loadMissingHeadsAsync(player, team, members, missingHeads, page);
         } else {
            this.openTeamGUINow(player, team, members, page, totalPages);
         }

      }
   }

   public void openTeamGUINow(Player player, DataManager team, List<String> members, int page, int totalPages) {
      String title = this.getTitle(page);
      Inventory gui = Bukkit.createInventory(new TeamGUIHolder(), 54, title);
      if (this.guiConfig.getBoolean("background.fill", false)) {
         Material backgroundMaterial = Material.getMaterial(this.guiConfig.getString("background.material", "BLACK_STAINED_GLASS_PANE"));
         if (backgroundMaterial != null) {
            ItemStack background = this.createItem(backgroundMaterial, HexUtils.colorize(this.guiConfig.getString("background.name", "")), this.guiConfig.getStringList("background.lore"));

            for(int i = 0; i < 54; ++i) {
               gui.setItem(i, background);
            }
         }
      }

      int start = page * 27;
      int end = Math.min(start + 27, members.size());
      int slot = 0;

      for(int i = start; i < end; ++i) {
         String memberName = (String)members.get(i);
         ItemStack head = this.guiManager.getCachedPlayerHead(memberName, team.getName());
         head = this.applySourceItemConfig(head, memberName, team.getName());
         gui.setItem(slot, head);
         ++slot;
      }

      this.addItemIfExists(gui, "search", team, player, page, totalPages);
      this.addItemIfExists(gui, "sort", team, player, page, totalPages);
      this.addItemIfExists(gui, "back", team, player, page, totalPages);
      this.addItemIfExists(gui, "team-info", team, player, page, totalPages);
      this.addItemIfExists(gui, "next", team, player, page, totalPages);
      if (this.isTeamHomeVisibleInGUI()) {
         this.addItemIfExists(gui, "team-home", team, player, page, totalPages);
      }

      this.addItemIfExists(gui, "pvp", team, player, page, totalPages);
      this.plugin.runTask(player, () -> player.openInventory(gui));
   }

   public void openPlaceholderGUI(Player player, DataManager team, List<String> members, int page) {
      int totalPages = Math.max(1, (int)Math.ceil((double)members.size() / (double)27.0F));
      if (page >= totalPages) {
         page = totalPages - 1;
      }

      if (page < 0) {
         page = 0;
      }

      String title = this.getTitle(page);
      Inventory gui = Bukkit.createInventory(new TeamGUIHolder(), 54, title);
      if (this.guiConfig.getBoolean("background.fill", false)) {
         Material backgroundMaterial = Material.getMaterial(this.guiConfig.getString("background.material", "BLACK_STAINED_GLASS_PANE"));
         if (backgroundMaterial != null) {
            ItemStack background = this.createItem(backgroundMaterial, HexUtils.colorize(this.guiConfig.getString("background.name", "")), this.guiConfig.getStringList("background.lore"));

            for(int i = 0; i < 54; ++i) {
               gui.setItem(i, background);
            }
         }
      }

      int start = page * 27;
      int end = Math.min(start + 27, members.size());
      int slot = 0;

      for(int i = start; i < end; ++i) {
         String memberName = (String)members.get(i);
         if (this.guiManager.isHeadCached(memberName)) {
            ItemStack head = this.guiManager.getCachedPlayerHead(memberName, team.getName());
            head = this.applySourceItemConfig(head, memberName, team.getName());
            gui.setItem(slot, head);
         } else {
            ItemStack placeholder = this.createItem(Material.PLAYER_HEAD, HexUtils.colorize("&8" + memberName), Arrays.asList(HexUtils.colorize("")));
            gui.setItem(slot, placeholder);
         }

         ++slot;
      }

      this.addItemIfExists(gui, "search", team, player, page, totalPages);
      this.addItemIfExists(gui, "sort", team, player, page, totalPages);
      this.addItemIfExists(gui, "back", team, player, page, totalPages);
      this.addItemIfExists(gui, "team-info", team, player, page, totalPages);
      this.addItemIfExists(gui, "next", team, player, page, totalPages);
      if (this.isTeamHomeVisibleInGUI()) {
         this.addItemIfExists(gui, "team-home", team, player, page, totalPages);
      }

      this.addItemIfExists(gui, "pvp", team, player, page, totalPages);
      this.plugin.runTask(player, () -> player.openInventory(gui));
   }

   private void addItemIfExists(Inventory gui, String itemName, DataManager team, Player player, int currentPage, int totalPages) {
      String path = "items." + itemName;
      if (this.guiConfig.contains(path + ".material")) {
         this.addItemFromConfig(gui, itemName, team, player, currentPage, totalPages);
      }
   }

   private void addItemFromConfig(Inventory gui, String itemName, DataManager team, Player player, int currentPage, int totalPages) {
      String path = "items." + itemName;
      Material material = Material.getMaterial(this.guiConfig.getString(path + ".material", "STONE"));
      if (material == null) {
         material = Material.STONE;
      }

      String displayName = this.guiConfig.getString(path + ".name", this.guiConfig.getString(path + ".display-name", ""));
      displayName = HexUtils.colorize(displayName.replace("%team%", team.getName()));
      List<String> lore = new ArrayList();
      if (this.guiConfig.contains(path + ".lore")) {
         for(String line : this.guiConfig.getStringList(path + ".lore")) {
            String processedLine = line.replace("%team%", team.getName()).replace("%current_page%", String.valueOf(currentPage + 1)).replace("%total_pages%", String.valueOf(totalPages));
            if (itemName.equals("pvp")) {
               String pvpStatus = team.getTeamConfig().getBoolean("pvp-enabled", false) ? this.guiConfig.getString("items.pvp.status.enabled", "&#34eb9b&lON") : this.guiConfig.getString("items.pvp.status.disabled", "&#FF0000&lOFF");
               processedLine = processedLine.replace("%pvp-status%", HexUtils.colorize(pvpStatus));
            }

            lore.add(HexUtils.colorize(processedLine));
         }
      }

      int slot = this.guiConfig.getInt(path + ".slot", 0);
      switch (itemName) {
         case "sort":
            TeamGUIManager.SortOption currentSort = this.guiManager.getPlayerSettings(player.getUniqueId()).getSortOption();
            if (!lore.isEmpty()) {
            }

            for(TeamGUIManager.SortOption option : this.configuredSortOrder) {
               String key = option.name();
               String optionName = this.guiConfig.getString("items.sort.options." + key, option.getDisplayName());
               String prefix = option == currentSort ? this.guiConfig.getString("items.sort.selected-prefix", "&#34eb9b• ") : this.guiConfig.getString("items.sort.unselected-prefix", "&f• ");
               lore.add(HexUtils.colorize(prefix + optionName));
            }
         default:
            ItemStack item = this.createItem(material, displayName, lore);
            gui.setItem(slot, item);
      }
   }

   private ItemStack applySourceItemConfig(ItemStack head, String playerName, String teamName) {
      String path = "items.source-item";
      if (!this.guiConfig.contains(path + ".display-name") && !this.guiConfig.contains(path + ".lore")) {
         return head;
      } else {
         ItemMeta meta = head.getItemMeta();
         if (meta == null) {
            return head;
         } else {
            if (this.guiConfig.contains(path + ".display-name")) {
               String displayName = this.guiConfig.getString(path + ".display-name", "&#34eb9b%player%");
               displayName = displayName.replace("%player%", playerName);
               meta.setDisplayName(HexUtils.colorize(displayName));
            }

            if (this.guiConfig.contains(path + ".lore")) {
               List<String> configLore = this.guiConfig.getStringList(path + ".lore");
               List<String> newLore = new ArrayList();
               Player onlinePlayer = Bukkit.getPlayer(playerName);

               for(String line : configLore) {
                  String processed = line.replace("%player%", playerName).replace("%status%", onlinePlayer != null ? "&aOnline" : "&cOffline");
                  newLore.add(HexUtils.colorize(processed));
               }

               meta.setLore(newLore);
            }

            head.setItemMeta(meta);
            return head;
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

   private String getTitle(int page) {
      String title = this.guiConfig.getString("gui.title", "&8ᴛᴇᴀᴍ (Page %current_page%)");
      return HexUtils.colorize(title.replace("%current_page%", String.valueOf(page + 1)));
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         if (event.getView().getTopInventory().getHolder() instanceof TeamGUIHolder) {
            event.setCancelled(true);
            Player player = (Player)event.getWhoClicked();
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
               int slot = event.getSlot();
               DataManager team = this.plugin.getTeamManager().getPlayerTeam(player.getName());
               if (team == null) {
                  player.closeInventory();
                  player.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
                  this.plugin.getSoundManager().playSound(player, "error-sound");
               } else {
                  TeamGUIManager.PlayerSettings settings = this.guiManager.getPlayerSettings(player.getUniqueId());
                  int currentPage = settings.getCurrentPage();
                  int totalPages = this.getTotalPages(team, player);
                  if (slot < 27) {
                     List<String> members = new ArrayList(team.getMembers());
                     String searchQuery = settings.getSearchQuery();
                     if (!searchQuery.isEmpty()) {
                        members.removeIf((member) -> !member.toLowerCase().contains(searchQuery.toLowerCase()));
                     }

                     this.guiManager.sortMembers(members, settings.getSortOption(), team.getName());
                     int index = currentPage * 27 + slot;
                     if (index < members.size()) {
                        String clickedMember = (String)members.get(index);
                        if (player.getName().equals(clickedMember)) {
                           this.plugin.getSoundManager().playSound(player, "error-sound");
                           player.sendMessage(this.plugin.getLangManager().getMessage("errors.cannot-manage-self"));
                           return;
                        }

                        if (!team.isLeader(player.getName())) {
                           boolean hasManagePerm = this.plugin.getRankManager().hasPermission(player.getName(), team.getName(), "manage_members");
                           if (!hasManagePerm) {
                              this.plugin.getSoundManager().playSound(player, "error-sound");
                              player.sendMessage(this.plugin.getLangManager().getMessage("errors.insufficient-rank"));
                              return;
                           }
                        }

                        if (this.plugin.getMemberManagerGUI() != null) {
                           this.plugin.getMemberManagerGUI().openMemberManager(player, clickedMember);
                           this.plugin.getSoundManager().playSound(player, "click-sound");
                           return;
                        }
                     }
                  }

                  String action = this.getActionForSlot(slot);
                  if (action != null) {
                     this.handleAction(player, team, action, currentPage, totalPages);
                  }

               }
            }
         }
      }
   }

   private String getActionForSlot(int slot) {
      if (slot == this.searchSlot && this.guiConfig.contains("items.search.material")) {
         return "SEARCH";
      } else if (slot == this.sortSlot && this.guiConfig.contains("items.sort.material")) {
         return "SORT";
      } else if (slot == this.backSlot && this.guiConfig.contains("items.back.material")) {
         return "BACK";
      } else if (slot == this.teamInfoSlot && this.guiConfig.contains("items.team-info.material")) {
         return "REFRESH";
      } else if (slot == this.nextSlot && this.guiConfig.contains("items.next.material")) {
         return "NEXT";
      } else if (slot == this.teamHomeSlot && this.guiConfig.contains("items.team-home.material") && this.isTeamHomeVisibleInGUI()) {
         return "HOME";
      } else {
         return slot == this.pvpSlot && this.guiConfig.contains("items.pvp.material") ? "PVP" : null;
      }
   }

   private void handleAction(Player player, DataManager team, String action, int currentPage, int totalPages) {
      this.plugin.getSoundManager().playSound(player, "click-sound");
      TeamGUIManager.PlayerSettings settings = this.guiManager.getPlayerSettings(player.getUniqueId());
      switch (action) {
         case "SEARCH":
            this.openSearchGUI(player);
            break;
         case "SORT":
            TeamGUIManager.SortOption currentSort = settings.getSortOption();
            TeamGUIManager.SortOption nextSort = this.getNextSortOption(currentSort);
            settings.setSortOption(nextSort);
            this.openTeamGUI(player, currentPage);
            break;
         case "BACK":
            if (currentPage > 0) {
               this.openTeamGUI(player, currentPage - 1);
            }
            break;
         case "REFRESH":
            this.openTeamGUI(player, currentPage);
            break;
         case "NEXT":
            if (currentPage < totalPages - 1) {
               this.openTeamGUI(player, currentPage + 1);
            }
            break;
         case "HOME":
            if (!this.plugin.getConfig().getBoolean("team-home.enabled", true)) {
               player.sendMessage(this.plugin.getLangManager().getMessage("errors.team-home-disabled"));
               this.plugin.getSoundManager().playSound(player, "error-sound");
               return;
            }

            if (team.hasHome()) {
               player.closeInventory();
               this.plugin.getTeamHome().teleportToHome(player);
            } else {
               player.closeInventory();
               player.performCommand("team sethome");
            }
            break;
         case "PVP":
            if (!team.isLeader(player.getName())) {
               boolean hasPerm = this.plugin.getRankManager().hasPermission(player.getName(), team.getName(), "pvp_toggle");
               if (!hasPerm) {
                  player.sendMessage(this.plugin.getLangManager().getMessage("errors.insufficient-rank"));
                  this.plugin.getSoundManager().playSound(player, "error-sound");
                  return;
               }
            }

            boolean currentPvP = team.getTeamConfig().getBoolean("pvp-enabled", false);
            boolean newPvP = !currentPvP;
            team.getTeamConfig().set("pvp-enabled", newPvP);
            team.saveToFile();
            String pvpMessage = newPvP ? this.plugin.getLangManager().getMessage("team.pvp-enabled") : this.plugin.getLangManager().getMessage("team.pvp-disabled");
            String colored = HexUtils.colorize(pvpMessage);
            team.broadcastToMembers(colored);

            for(String memberName : team.getMembers()) {
               Player mem = Bukkit.getPlayer(memberName);
               if (mem != null && mem.isOnline()) {
                  this.plugin.getSoundManager().playSound(mem, "success-sound");
               }
            }

            this.openTeamGUI(player, currentPage);
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player) {
         if (event.getView().getTopInventory().getHolder() instanceof TeamGUIHolder) {
            Player player = (Player)event.getPlayer();
            this.plugin.runTaskLater(player, () -> {
               if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof TeamGUIHolder)) {
                  this.guiManager.resetPlayerSettings(player);
               }

            }, 1L);
         }
      }
   }

   private void openSearchGUI(Player player) {
      boolean useSignAPI = this.guiConfig.getBoolean("chat-input.SignAPI", true);
      boolean useChatAPI = this.guiConfig.getBoolean("chat-input.ChatAPI", false);
      player.closeInventory();
      if (useSignAPI && !useChatAPI) {
         this.openSignSearch(player);
      } else if (!useSignAPI && useChatAPI) {
         this.openChatSearch(player);
      } else {
         this.openSignSearch(player);
      }

   }

   private void openSignSearch(Player player) {
      String[] lines = new String[4];

      for(int i = 0; i < Math.min(4, this.signGuiLines.size()); ++i) {
         lines[i] = (String)this.signGuiLines.get(i);
      }

      this.plugin.getSignManager().openSignEditor(player, (String[])lines, (p, input) -> {
         String searchTerm = input.trim();
         if (searchTerm.isEmpty()) {
            this.plugin.runTask(p, () -> this.openTeamGUI(p, 0));
         } else {
            if (searchTerm.equalsIgnoreCase("clear")) {
               this.guiManager.setSearchQuery(p.getUniqueId(), "");
               this.plugin.getSoundManager().playSound(p, "success-sound");
            } else {
               this.guiManager.setSearchQuery(p.getUniqueId(), searchTerm);
               this.plugin.getSoundManager().playSound(p, "click-sound");
            }

            this.plugin.runTask(p, () -> this.openTeamGUI(p, 0));
         }
      });
   }

   private void openChatSearch(Player player) {
      UUID playerId = player.getUniqueId();
      if (!this.guiManager.isWaitingForChatInput(playerId)) {
         this.guiManager.setWaitingForChatInput(playerId, true);
         player.sendMessage(this.plugin.getLangManager().getMessage("team.team-search"));
         ChatInputListener listener = new ChatInputListener(this.plugin, this, player, this.guiManager);
         this.plugin.getServer().getPluginManager().registerEvents(listener, this.plugin);
         this.plugin.getSoundManager().playSound(player, "click-sound");
      }
   }

   private int getTotalPages(DataManager team, Player player) {
      List<String> members = new ArrayList(team.getMembers());
      String searchQuery = this.guiManager.getPlayerSettings(player.getUniqueId()).getSearchQuery();
      if (!searchQuery.isEmpty()) {
         members.removeIf((member) -> !member.toLowerCase().contains(searchQuery.toLowerCase()));
      }

      return Math.max(1, (int)Math.ceil((double)members.size() / (double)27.0F));
   }

   private TeamGUIManager.SortOption getNextSortOption(TeamGUIManager.SortOption current) {
      if (this.configuredSortOrder != null && !this.configuredSortOrder.isEmpty()) {
         int idx = this.configuredSortOrder.indexOf(current);
         return idx == -1 ? (TeamGUIManager.SortOption)this.configuredSortOrder.get(0) : (TeamGUIManager.SortOption)this.configuredSortOrder.get((idx + 1) % this.configuredSortOrder.size());
      } else {
         TeamGUIManager.SortOption[] values = TeamGUIManager.SortOption.values();
         int nextIndex = (current.ordinal() + 1) % values.length;
         return values[nextIndex];
      }
   }

   public void setSearchQuery(Player player, String query) {
      this.guiManager.setSearchQuery(player.getUniqueId(), query);
      this.openTeamGUI(player, 0);
   }

   public void clearHeadCache() {
      this.guiManager.clearHeadCache();
   }

   public void removeFromHeadCache(String playerName) {
      this.guiManager.removeFromHeadCache(playerName);
   }

   public void reloadConfig() {
      this.loadGUIConfig();
   }

   public TeamGUIManager getGuiManager() {
      return this.guiManager;
   }

   private static class ChatInputListener implements Listener {
      private final Team plugin;
      private final TeamGUI gui;
      private final Player player;
      private final UUID playerId;
      private final TeamGUIManager guiManager;
      private boolean active = true;

      public ChatInputListener(Team plugin, TeamGUI gui, Player player, TeamGUIManager guiManager) {
         this.plugin = plugin;
         this.gui = gui;
         this.player = player;
         this.playerId = player.getUniqueId();
         this.guiManager = guiManager;
      }

      @EventHandler
      public void onPlayerChat(AsyncPlayerChatEvent event) {
         if (this.active) {
            if (event.getPlayer().equals(this.player)) {
               event.setCancelled(true);
               String message = event.getMessage().trim();
               this.active = false;
               AsyncPlayerChatEvent.getHandlerList().unregister(this);
               PlayerQuitEvent.getHandlerList().unregister(this);
               this.plugin.runTask(this.player, () -> {
                  if (!message.equalsIgnoreCase("cancel") && !message.equalsIgnoreCase("")) {
                     if (message.equalsIgnoreCase("clear")) {
                        this.guiManager.setSearchQuery(this.playerId, "");
                        this.plugin.getSoundManager().playSound(this.player, "success-sound");
                        this.player.sendMessage(this.plugin.getLangManager().getMessage("team.search-cancelled"));
                     } else {
                        this.guiManager.setSearchQuery(this.playerId, message);
                        this.plugin.getSoundManager().playSound(this.player, "click-sound");
                     }

                     this.guiManager.setWaitingForChatInput(this.playerId, false);
                     this.gui.openTeamGUI(this.player, 0);
                  } else {
                     this.player.sendMessage(this.plugin.getLangManager().getMessage("team.search-cancelled"));
                     this.guiManager.setWaitingForChatInput(this.playerId, false);
                     this.gui.openTeamGUI(this.player, 0);
                  }
               });
            }
         }
      }

      @EventHandler
      public void onPlayerQuit(PlayerQuitEvent event) {
         if (this.active) {
            if (event.getPlayer().equals(this.player)) {
               this.active = false;
               AsyncPlayerChatEvent.getHandlerList().unregister(this);
               PlayerQuitEvent.getHandlerList().unregister(this);
               this.guiManager.setWaitingForChatInput(this.playerId, false);
            }
         }
      }
   }
}
