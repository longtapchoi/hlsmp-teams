package de.elivb.hlsmpTeams;

import de.elivb.hlsmpTeams.GUI.MemberManagerGUI;
import de.elivb.hlsmpTeams.GUI.TeamGUI;
import de.elivb.hlsmpTeams.Manager.DataManager;
import de.elivb.hlsmpTeams.Manager.HomeManager;
import de.elivb.hlsmpTeams.Manager.LangManager;
import de.elivb.hlsmpTeams.Manager.SignManager;
import de.elivb.hlsmpTeams.Manager.SoundManager;
import de.elivb.hlsmpTeams.Manager.TeamManager;
import de.elivb.hlsmpTeams.Manager.TeamRankManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class Team extends JavaPlugin implements Listener, TabCompleter {
   private TeamManager teamManager;
   private LangManager langManager;
   private HomeManager teamHome;
   private TeamRankManager rankManager;
   private TeamGUI teamGUI;
   private MemberManagerGUI memberManagerGUI;
   private SoundManager soundManager;
   private SignManager signManager;
   private Map<String, DataManager> playerTeams;
   private boolean isFolia;
   private LicenseManager licenseManager;
   private Set<UUID> teamChatEnabled;

   // Toggle sets cho HLSMP-Settings
   private Set<UUID> teamInviteDisabled;   // teamtoggle off = không nhận invite
   private Set<UUID> teamChatToggleOff;    // teamchattoggle off

   public void onEnable() {
      this.licenseManager = new LicenseManager(this);
      if (this.licenseManager.validateLicenseOnStartup()) {
         this.saveDefaultConfig();
         this.reloadConfig();
         this.isFolia = this.checkFolia();
         this.langManager = new LangManager(this);
         this.teamManager = new TeamManager(this);
         this.teamHome = new HomeManager(this);
         this.rankManager = new TeamRankManager(this);
         this.signManager = new SignManager(this);
         this.teamGUI = new TeamGUI(this);
         this.memberManagerGUI = new MemberManagerGUI(this);
         this.soundManager = new SoundManager(this);
         this.playerTeams = new HashMap<>();
         this.teamChatEnabled = new HashSet<>();
         this.teamInviteDisabled = new HashSet<>();
         this.teamChatToggleOff = new HashSet<>();
         this.getServer().getPluginManager().registerEvents(this, this);
         this.getServer().getPluginManager().registerEvents(new PvPListener(this), this);
         this.getServer().getPluginManager().registerEvents(new TeamChatListener(this), this);
         this.langManager.loadMessages();
         this.teamManager.loadAllTeams();
         this.rankManager.loadAllPermissions();
         if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Papi1(this).register();
         }
      }
   }

   private boolean checkFolia() {
      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         return true;
      } catch (ClassNotFoundException e) {
         return false;
      }
   }

   public void onDisable() {
      if (this.rankManager != null) this.rankManager.saveAllPermissions();
      if (this.teamGUI != null) this.teamGUI.clearHeadCache();
      if (this.teamHome != null) this.teamHome.onDisable();
   }

   // ==================== TEAM INVITE TOGGLE ====================
   public boolean isTeamInviteEnabled(Player player) {
      return !this.teamInviteDisabled.contains(player.getUniqueId());
   }

   public void setTeamInviteEnabled(Player player, boolean enabled) {
      if (enabled) {
         this.teamInviteDisabled.remove(player.getUniqueId());
      } else {
         this.teamInviteDisabled.add(player.getUniqueId());
      }
   }

   // ==================== TEAM CHAT TOGGLE (standalone) ====================
   public void setTeamChatEnabled(Player player, boolean enabled) {
      if (enabled) {
         this.teamChatEnabled.add(player.getUniqueId());
         this.teamChatToggleOff.remove(player.getUniqueId());
      } else {
         this.teamChatEnabled.remove(player.getUniqueId());
         this.teamChatToggleOff.add(player.getUniqueId());
      }
   }

   public boolean isTeamChatEnabled(Player player) {
      return this.teamChatEnabled.contains(player.getUniqueId());
   }

   public void sendTeamMessage(Player sender, String message) {
      DataManager team = this.teamManager.getPlayerTeam(sender.getName());
      if (team == null) {
         sender.sendMessage(this.langManager.getMessage("errors.not-in-team"));
         this.soundManager.playSound(sender, "error-sound");
      } else {
         String formattedMessage = this.langManager.getMessage("team.chat-format")
               .replace("%player%", sender.getName())
               .replace("%message%", message)
               .replace("%team%", team.getName());
         String coloredMessage = HexUtils.colorize(formattedMessage);
         for (String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayer(memberName);
            if (member != null && member.isOnline()) {
               member.sendMessage(coloredMessage);
            }
         }
      }
   }

   public LicenseManager getLicenseManager() { return this.licenseManager; }
   private boolean isTeamHomeEnabled() { return this.getConfig().getBoolean("team-home.enabled", true); }

   // ==================== COMMANDS ====================
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      String cmdName = command.getName().toLowerCase();

      // /teamtoggle — toggle nhận lời mời team
      if (cmdName.equals("teamtoggle")) {
         if (!(sender instanceof Player player)) return true;
         boolean newVal;
         if (args.length > 0 && args[0].equalsIgnoreCase("on")) {
            newVal = true;
         } else if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
            newVal = false;
         } else {
            newVal = !isTeamInviteEnabled(player);
         }
         setTeamInviteEnabled(player, newVal);
         if (newVal) {
            player.sendMessage(this.langManager.getMessage("team.invite-toggle-on"));
         } else {
            player.sendMessage(this.langManager.getMessage("team.invite-toggle-off"));
         }
         this.soundManager.playSound(player, "success-sound");
         return true;
      }

      // /teamchattoggle — toggle team chat (standalone cho HLSMP-Settings)
      if (cmdName.equals("teamchattoggle")) {
         if (!(sender instanceof Player player)) return true;
         boolean newVal;
         if (args.length > 0 && args[0].equalsIgnoreCase("on")) {
            newVal = true;
         } else if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
            newVal = false;
         } else {
            newVal = !isTeamChatEnabled(player);
         }
         setTeamChatEnabled(player, newVal);
         if (newVal) {
            player.sendMessage(this.langManager.getMessage("team.chat-enabled"));
         } else {
            player.sendMessage(this.langManager.getMessage("team.chat-disabled"));
         }
         this.soundManager.playSound(player, "success-sound");
         return true;
      }

      // /team
      if (cmdName.equals("team")) {
         if (args.length == 0) {
            if (sender instanceof Player player) {
               // Mở GUI + hiện danh sách lệnh trong chat
               this.teamGUI.openTeamGUI(player);
               this.showHelp(player);
            }
            return true;
         }

         if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("team.admin")) {
               if (sender instanceof Player p) {
                  sender.sendMessage(this.langManager.getMessage("errors.insufficient-rank"));
                  this.soundManager.playSound(p, "error-sound");
               }
               return true;
            }
            this.reloadConfig();
            this.langManager.reloadMessages();
            this.teamManager.loadAllTeams();
            this.rankManager.loadAllPermissions();
            this.teamGUI.reloadConfig();
            this.memberManagerGUI.reloadConfig();
            this.teamManager.reloadBlockedTeamNames();
            this.teamManager.reloadTeamNameLengthSettings();
            if (this.teamHome != null) this.teamHome.reloadSettings();
            if (sender instanceof Player p) {
               sender.sendMessage(this.langManager.getMessage("team.plugin-reloaded"));
               this.soundManager.playSound(p, "plugin-reloaded");
            } else {
               sender.sendMessage(this.langManager.getMessage("team.plugin-reloaded"));
            }
            return true;
         }

         if (!(sender instanceof Player)) return true;
         Player player = (Player) sender;

         switch (args[0].toLowerCase()) {
            case "create":
               if (args.length < 2) return true;
               this.teamManager.createTeam(player, args[1]);
               break;
            case "invite":
               if (args.length < 2) return true;
               this.teamManager.invitePlayer(player, args[1]);
               break;
            case "join":
               this.teamManager.acceptInvite(player);
               break;
            case "leave":
               this.teamManager.leaveTeam(player);
               break;
            case "kick":
               if (args.length < 2) return true;
               this.teamManager.kickPlayer(player, args[1]);
               break;
            case "delete":
               this.teamManager.disbandTeam(player);
               break;
            case "sethome":
               if (!this.isTeamHomeEnabled()) {
                  player.sendMessage(this.langManager.getMessage("errors.team-home-disabled"));
                  this.soundManager.playSound(player, "error-sound");
               } else {
                  this.teamHome.setHome(player);
               }
               break;
            case "home":
               if (!this.isTeamHomeEnabled()) {
                  player.sendMessage(this.langManager.getMessage("errors.team-home-disabled"));
                  this.soundManager.playSound(player, "error-sound");
               } else {
                  this.teamHome.teleportToHome(player);
               }
               break;
            case "delhome":
               if (!this.isTeamHomeEnabled()) {
                  player.sendMessage(this.langManager.getMessage("errors.team-home-disabled"));
                  this.soundManager.playSound(player, "error-sound");
               } else {
                  this.teamHome.deleteHome(player);
               }
               break;
            case "transfer":
               if (args.length < 2) return true;
               this.teamManager.transferLeadership(player, args[1]);
               break;
            case "gui":
               this.teamGUI.openTeamGUI(player);
               break;
            case "chat":
               if (args.length == 1 || (args.length >= 2 && args[1].equalsIgnoreCase("toggle"))) {
                  boolean isEnabled = this.isTeamChatEnabled(player);
                  this.setTeamChatEnabled(player, !isEnabled);
                  player.sendMessage(this.langManager.getMessage(!isEnabled ? "team.chat-enabled" : "team.chat-disabled"));
               } else {
                  String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                  this.sendTeamMessage(player, message);
               }
               break;
            default:
               this.showHelp(player);
         }
      }

      return true;
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
      String cmdName = command.getName().toLowerCase();

      if (cmdName.equals("teamtoggle") || cmdName.equals("teamchattoggle")) {
         if (args.length == 1) {
            for (String opt : List.of("on", "off")) {
               if (opt.startsWith(args[0].toLowerCase())) completions.add(opt);
            }
         }
         return completions;
      }

      if (cmdName.equals("team")) {
         if (args.length == 1) {
            List<String> commands = new ArrayList<>(Arrays.asList("create", "invite", "join", "leave", "kick", "delete", "transfer", "gui", "chat"));
            if (this.isTeamHomeEnabled()) commands.addAll(Arrays.asList("sethome", "home", "delhome"));
            if (sender.hasPermission("team.admin")) commands.add("reload");
            for (String cmd : commands) {
               if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) completions.add(cmd);
            }
         } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
               case "invite": case "kick": case "transfer":
                  for (Player p : Bukkit.getOnlinePlayers()) {
                     if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(p.getName());
                  }
                  break;
               case "join":
                  if (sender instanceof Player player) {
                     String pending = this.teamManager.getPendingInvite(player.getName());
                     if (pending != null && pending.toLowerCase().startsWith(args[1].toLowerCase())) completions.add(pending);
                  }
                  break;
               case "chat":
                  completions.add("toggle");
                  break;
            }
         }
      }
      return completions;
   }

   private void showHelp(Player player) {
      String normalHelp = this.langManager.getMessageWithoutPrefix("command-usage");
      for (String line : normalHelp.split("\n")) player.sendMessage(line);
      if (player.hasPermission("team.admin")) {
         player.sendMessage("");
         String adminHelp = this.langManager.getMessageWithoutPrefix("command-usage-admin");
         for (String line : adminHelp.split("\n")) player.sendMessage(line);
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      this.teamManager.getPlayerTeam(player.getName());
      this.teamChatEnabled.remove(player.getUniqueId());
   }

   // ==================== SCHEDULER UTILS ====================
   public void runTaskAsynchronously(Runnable task) {
      if (this.isFolia) this.getServer().getAsyncScheduler().runNow(this, (t) -> task.run());
      else this.getServer().getScheduler().runTaskAsynchronously(this, task);
   }

   public void runTaskLaterAsynchronously(Runnable task, long delay) {
      if (this.isFolia) this.getServer().getAsyncScheduler().runDelayed(this, (t) -> task.run(), delay * 50L, TimeUnit.MILLISECONDS);
      else this.getServer().getScheduler().runTaskLaterAsynchronously(this, task, delay);
   }

   public void runTaskTimerAsynchronously(Runnable task, long delay, long period) {
      if (this.isFolia) this.getServer().getAsyncScheduler().runAtFixedRate(this, (t) -> task.run(), delay * 50L, period * 50L, TimeUnit.MILLISECONDS);
      else this.getServer().getScheduler().runTaskTimerAsynchronously(this, task, delay, period);
   }

   public void runTask(Player player, Runnable task) {
      if (this.isFolia) player.getScheduler().run(this, (t) -> task.run(), null);
      else this.getServer().getScheduler().runTask(this, task);
   }

   public void runTask(Runnable task) {
      if (this.isFolia) this.getServer().getGlobalRegionScheduler().run(this, (t) -> task.run());
      else this.getServer().getScheduler().runTask(this, task);
   }

   public void runTaskLater(Player player, Runnable task, long delay) {
      if (this.isFolia) player.getScheduler().runDelayed(this, (t) -> task.run(), null, delay);
      else this.getServer().getScheduler().runTaskLater(this, task, delay);
   }

   public void runTaskLater(Runnable task, long delay) {
      if (this.isFolia) this.getServer().getGlobalRegionScheduler().runDelayed(this, (t) -> task.run(), delay);
      else this.getServer().getScheduler().runTaskLater(this, task, delay);
   }

   public void runTaskTimer(Player player, Runnable task, long delay, long period) {
      if (this.isFolia) player.getScheduler().runAtFixedRate(this, (t) -> task.run(), null, delay, period);
      else this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
   }

   public void runTaskTimer(Runnable task, long delay, long period) {
      if (this.isFolia) this.getServer().getGlobalRegionScheduler().runAtFixedRate(this, (t) -> task.run(), delay, period);
      else this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
   }

   public void runTaskAtLocation(Location location, Runnable task) {
      if (this.isFolia) this.getServer().getRegionScheduler().run(this, location, (t) -> task.run());
      else this.getServer().getScheduler().runTask(this, task);
   }

   public void runTaskLaterAtLocation(Location location, Runnable task, long delay) {
      if (this.isFolia) this.getServer().getRegionScheduler().runDelayed(this, location, (t) -> task.run(), delay);
      else this.getServer().getScheduler().runTaskLater(this, task, delay);
   }

   public void runTaskTimerAtLocation(Location location, Runnable task, long delay, long period) {
      if (this.isFolia) this.getServer().getRegionScheduler().runAtFixedRate(this, location, (t) -> task.run(), delay, period);
      else this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
   }

   public Object runGlobalTimer(Runnable task, long delay, long period) {
      return this.isFolia
            ? this.getServer().getGlobalRegionScheduler().runAtFixedRate(this, (t) -> task.run(), delay, period)
            : this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
   }

   public void cancelTask(Object task) {
      if (task == null) return;
      if (this.isFolia) {
         if (task instanceof ScheduledTask t) t.cancel();
      } else if (task instanceof BukkitTask t) {
         t.cancel();
      } else if (task instanceof Integer i) {
         this.getServer().getScheduler().cancelTask(i);
      } else if (task instanceof Number n) {
         this.getServer().getScheduler().cancelTask(n.intValue());
      }
   }

   public void cancelAllTasks() {
      if (this.isFolia) {
         this.getServer().getGlobalRegionScheduler().cancelTasks(this);
         this.getServer().getAsyncScheduler().cancelTasks(this);
      } else {
         this.getServer().getScheduler().cancelTasks(this);
      }
   }

   public boolean isFolia() { return this.isFolia; }
   public TeamGUI getTeamGUI() { return this.teamGUI; }
   public MemberManagerGUI getMemberManagerGUI() { return this.memberManagerGUI; }
   public TeamManager getTeamManager() { return this.teamManager; }
   public LangManager getLangManager() { return this.langManager; }
   public HomeManager getTeamHome() { return this.teamHome; }
   public TeamRankManager getRankManager() { return this.rankManager; }
   public SoundManager getSoundManager() { return this.soundManager; }
   public SignManager getSignManager() { return this.signManager; }
   public Map<String, DataManager> getPlayerTeams() { return this.playerTeams; }
}
