package de.elivb.hlsmpTeams.Manager;

import de.elivb.hlsmpTeams.HexUtils;
import de.elivb.hlsmpTeams.Team;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TeamManager {
   private Team plugin;
   private Map<String, DataManager> teams;
   private Map<String, String> pendingInvites;
   private Map<String, ChatColor> teamColors;
   private List<String> blockedTeamNames;
   private int minTeamNameLength;
   private int maxTeamNameLength;

   public TeamManager(Team plugin) {
      this.plugin = plugin;
      this.teams = new HashMap();
      this.pendingInvites = new HashMap();
      this.teamColors = new HashMap();
      this.blockedTeamNames = new ArrayList();
      this.minTeamNameLength = 3;
      this.maxTeamNameLength = 9;
      this.initializeColors();
      this.loadBlockedTeamNames();
      this.loadTeamNameLengthSettings();
   }

   private void initializeColors() {
      this.teamColors.put("RED", ChatColor.RED);
      this.teamColors.put("BLUE", ChatColor.BLUE);
      this.teamColors.put("GREEN", ChatColor.GREEN);
      this.teamColors.put("YELLOW", ChatColor.YELLOW);
      this.teamColors.put("AQUA", ChatColor.AQUA);
      this.teamColors.put("PURPLE", ChatColor.LIGHT_PURPLE);
      this.teamColors.put("GOLD", ChatColor.GOLD);
      this.teamColors.put("DARK_RED", ChatColor.DARK_RED);
      this.teamColors.put("DARK_BLUE", ChatColor.DARK_BLUE);
      this.teamColors.put("DARK_GREEN", ChatColor.DARK_GREEN);
      this.teamColors.put("DARK_PURPLE", ChatColor.DARK_PURPLE);
   }

   private void loadBlockedTeamNames() {
      this.blockedTeamNames = this.plugin.getConfig().getStringList("blocked-team-names");
      if (this.blockedTeamNames == null) {
         this.blockedTeamNames = new ArrayList();
      }

      this.blockedTeamNames.replaceAll(String::toLowerCase);
   }

   private void loadTeamNameLengthSettings() {
      this.minTeamNameLength = this.plugin.getConfig().getInt("min-team-name-length", 3);
      this.maxTeamNameLength = this.plugin.getConfig().getInt("max-team-name-length", 9);
   }

   public void reloadBlockedTeamNames() {
      this.loadBlockedTeamNames();
   }

   public void reloadTeamNameLengthSettings() {
      this.loadTeamNameLengthSettings();
   }

   private boolean containsBlockedWord(String teamName) {
      String lowerCaseName = teamName.toLowerCase();

      for(String blockedWord : this.blockedTeamNames) {
         if (lowerCaseName.contains(blockedWord)) {
            return true;
         }
      }

      return false;
   }

   private boolean isValidNameLength(String teamName) {
      String cleanName = teamName.replaceAll("&#[A-Fa-f0-9]{6}", "").replaceAll("&[0-9a-fk-or]", "").trim();
      int length = cleanName.length();
      return length >= this.minTeamNameLength && length <= this.maxTeamNameLength;
   }

   private void playNoPermission(Player player) {
      if (player != null) {
         try {
            player.playSound(player.getLocation(), "entity.villager.no", 1.0F, 1.0F);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

   }

   public void createTeam(Player leader, String teamName) {
      String cleanTeamName = teamName.replaceAll("&#[A-Fa-f0-9]{6}", "").trim();
      if (!cleanTeamName.isEmpty()) {
         if (!this.isValidNameLength(cleanTeamName)) {
            String msg = this.plugin.getLangManager().getMessage("errors.invalid-team-name-length").replace("%min%", String.valueOf(this.minTeamNameLength)).replace("%max%", String.valueOf(this.maxTeamNameLength));
            leader.sendMessage(msg);
            this.playNoPermission(leader);
         } else if (this.containsBlockedWord(cleanTeamName)) {
            leader.sendMessage(this.plugin.getLangManager().getMessage("errors.blocked-team-name"));
            this.playNoPermission(leader);
         } else if (!DataManager.teamExists(cleanTeamName, this.plugin) && !this.teams.containsKey(cleanTeamName.toUpperCase())) {
            if (this.getPlayerTeam(leader.getName()) != null) {
               leader.sendMessage(this.plugin.getLangManager().getMessage("errors.already-in-team"));
               this.playNoPermission(leader);
            } else {
               DataManager team = new DataManager(cleanTeamName, leader.getName(), this.plugin);
               this.teams.put(cleanTeamName.toUpperCase(), team);
               this.plugin.getPlayerTeams().put(leader.getName(), team);
               leader.sendMessage(this.plugin.getLangManager().getMessage("team.created").replace("%team%", HexUtils.colorize(teamName)));
            }
         } else {
            leader.sendMessage(this.plugin.getLangManager().getMessage("errors.team-already-exists"));
            this.playNoPermission(leader);
         }
      }
   }

   public void invitePlayer(Player inviter, String targetName) {
      DataManager team = this.getPlayerTeam(inviter.getName());
      if (team == null) {
         inviter.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
         this.playNoPermission(inviter);
      } else if (!team.isLeader(inviter.getName())) {
         inviter.sendMessage(this.plugin.getLangManager().getMessage("errors.not-leader"));
         this.playNoPermission(inviter);
      } else {
         Player target = Bukkit.getPlayer(targetName);
         if (target == null) {
            inviter.sendMessage(this.plugin.getLangManager().getMessage("errors.player-not-found"));
            this.playNoPermission(inviter);
         } else if (this.getPlayerTeam(target.getName()) != null) {
            inviter.sendMessage(this.plugin.getLangManager().getMessage("errors.player-already-in-team"));
            this.playNoPermission(inviter);
         } else if (!this.plugin.isTeamInviteEnabled(target)) {
            inviter.sendMessage(this.plugin.getLangManager().getMessage("errors.player-invite-disabled").replace("%player%", target.getName()));
            this.playNoPermission(inviter);
         } else {
            this.pendingInvites.put(target.getName(), team.getName());
            inviter.sendMessage(this.plugin.getLangManager().getMessage("team.invite-sent").replace("%player%", target.getName()));

            for(String line : this.plugin.getLangManager().getMessages("team.invite-received")) {
               target.sendMessage(line.replace("%team%", team.getName()).replace("%inviter%", inviter.getName()));
            }

         }
      }
   }

   public void acceptInvite(Player player) {
      String teamName = (String)this.pendingInvites.get(player.getName());
      if (teamName == null) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.no-pending-invites"));
         this.playNoPermission(player);
      } else {
         DataManager team = (DataManager)this.teams.get(teamName.toUpperCase());
         if (team == null) {
            player.sendMessage(this.plugin.getLangManager().getMessage("errors.team-not-found"));
            this.playNoPermission(player);
            this.pendingInvites.remove(player.getName());
         } else if (team.getMemberCount() >= 27) {
            player.sendMessage(this.plugin.getLangManager().getMessage("errors.team-full"));
            this.playNoPermission(player);
            this.pendingInvites.remove(player.getName());
         } else {
            team.addMember(player.getName());
            this.plugin.getPlayerTeams().put(player.getName(), team);
            this.pendingInvites.remove(player.getName());
            player.sendMessage(this.plugin.getLangManager().getMessage("team.joined").replace("%team%", teamName));
            team.broadcastToMembers(this.plugin.getLangManager().getMessage("team.member-joined").replace("%player%", player.getName()));
         }
      }
   }

   public void leaveTeam(Player player) {
      DataManager team = this.getPlayerTeam(player.getName());
      if (team == null) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
         this.playNoPermission(player);
      } else if (team.isLeader(player.getName())) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.leader-cant-leave"));
         this.playNoPermission(player);
      } else {
         team.removeMember(player.getName());
         this.plugin.getPlayerTeams().remove(player.getName());
         this.plugin.getRankManager().removePlayerPermissions(player.getName(), team.getName());
         player.sendMessage(this.plugin.getLangManager().getMessage("team.left"));
         team.broadcastToMembers(this.plugin.getLangManager().getMessage("team.member-left").replace("%player%", player.getName()));
      }
   }

   public void kickPlayer(Player kicker, String targetName) {
      DataManager team = this.getPlayerTeam(kicker.getName());
      if (team == null) {
         kicker.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
         this.playNoPermission(kicker);
      } else if (!team.isLeader(kicker.getName())) {
         kicker.sendMessage(this.plugin.getLangManager().getMessage("errors.not-leader"));
         this.playNoPermission(kicker);
      } else {
         Player target = Bukkit.getPlayer(targetName);
         if (target == null) {
            kicker.sendMessage(this.plugin.getLangManager().getMessage("errors.player-not-found"));
            this.playNoPermission(kicker);
         } else if (!team.isMember(target.getName())) {
            kicker.sendMessage(this.plugin.getLangManager().getMessage("errors.player-not-in-your-team"));
            this.playNoPermission(kicker);
         } else if (team.isLeader(target.getName())) {
            kicker.sendMessage(this.plugin.getLangManager().getMessage("errors.cannot-kick"));
            this.playNoPermission(kicker);
         } else {
            team.removeMember(target.getName());
            this.plugin.getPlayerTeams().remove(target.getName());
            this.plugin.getRankManager().removePlayerPermissions(target.getName(), team.getName());
            kicker.sendMessage(this.plugin.getLangManager().getMessage("team.player-kicked").replace("%player%", target.getName()));
            target.sendMessage(this.plugin.getLangManager().getMessage("team.you-were-kicked").replace("%team%", team.getName()));
            team.broadcastToMembers(this.plugin.getLangManager().getMessage("team.member-left").replace("%player%", target.getName()));
         }
      }
   }

   public void disbandTeam(Player player) {
      DataManager team = this.getPlayerTeam(player.getName());
      if (team == null) {
         player.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
         this.playNoPermission(player);
      } else {
         if (!team.isLeader(player.getName())) {
            boolean hasDeletePerm = this.plugin.getRankManager().hasPermission(player.getName(), team.getName(), "delete_team");
            if (!hasDeletePerm) {
               player.sendMessage(this.plugin.getLangManager().getMessage("errors.insufficient-rank"));
               this.playNoPermission(player);
               return;
            }
         }

         String teamName = team.getName();
         this.plugin.getTeamHome().removeTeamHome(teamName);
         this.plugin.getRankManager().removeTeamPermissions(teamName);

         for(String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayer(memberName);
            if (member != null) {
               this.plugin.getPlayerTeams().remove(memberName);
            }
         }

         this.pendingInvites.entrySet().removeIf((entry) -> ((String)entry.getValue()).equalsIgnoreCase(teamName));
         team.deleteTeamFile();
         this.teams.remove(teamName.toUpperCase());
         player.sendMessage(this.plugin.getLangManager().getMessage("team.deleted").replace("%team%", teamName));
      }
   }

   public void transferLeadership(Player currentLeader, String newLeaderName) {
      DataManager team = this.getPlayerTeam(currentLeader.getName());
      if (team == null) {
         currentLeader.sendMessage(this.plugin.getLangManager().getMessage("errors.not-in-team"));
         this.playNoPermission(currentLeader);
      } else if (!team.isLeader(currentLeader.getName())) {
         currentLeader.sendMessage(this.plugin.getLangManager().getMessage("errors.not-leader"));
         this.playNoPermission(currentLeader);
      } else {
         Player newLeader = Bukkit.getPlayer(newLeaderName);
         if (newLeader == null) {
            currentLeader.sendMessage(this.plugin.getLangManager().getMessage("errors.player-not-found"));
            this.playNoPermission(currentLeader);
         } else if (!team.isMember(newLeader.getName())) {
            currentLeader.sendMessage(this.plugin.getLangManager().getMessage("errors.player-not-in-your-team"));
            this.playNoPermission(currentLeader);
         } else {
            team.setLeader(newLeader.getName());
            this.plugin.getRankManager().removePlayerPermissions(currentLeader.getName(), team.getName());
            team.broadcastToMembers(this.plugin.getLangManager().getMessage("team.new-owner").replace("%player%", newLeader.getName()));
         }
      }
   }

   public DataManager getPlayerTeam(String playerName) {
      return (DataManager)this.plugin.getPlayerTeams().get(playerName);
   }

   public DataManager getTeamByName(String teamName) {
      return (DataManager)this.teams.get(teamName.toUpperCase());
   }

   public boolean hasPendingInvite(String playerName) {
      return this.pendingInvites.containsKey(playerName);
   }

   public String getPendingInvite(String playerName) {
      return (String)this.pendingInvites.get(playerName);
   }

   public void removePendingInvite(String playerName) {
      this.pendingInvites.remove(playerName);
   }

   public Set<String> getAllTeamNames() {
      return this.teams.keySet();
   }

   public Map<String, DataManager> getAllTeams() {
      return new HashMap(this.teams);
   }

   public int getTotalTeams() {
      return this.teams.size();
   }

   public int getTotalPlayersInTeams() {
      return this.plugin.getPlayerTeams().size();
   }

   private ChatColor getRandomColor() {
      ArrayList<ChatColor> colors = new ArrayList(this.teamColors.values());
      Random random = new Random();
      return (ChatColor)colors.get(random.nextInt(colors.size()));
   }

   public void loadAllTeams() {
      try {
         for(String teamName : DataManager.getAllTeamNames(this.plugin)) {
            try {
               DataManager team = new DataManager(teamName, this.plugin);
               this.teams.put(teamName.toUpperCase(), team);

               for(String memberName : team.getMembers()) {
                  this.plugin.getPlayerTeams().put(memberName, team);
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }
}
