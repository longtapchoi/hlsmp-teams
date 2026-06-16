package de.elivb.hlsmpTeams.Manager;

import de.elivb.hlsmpTeams.Team;
import org.bukkit.configuration.file.FileConfiguration;

public class TeamRankManager {
   private final Team plugin;
   public static final String PERM_PVP_TOGGLE = "pvp_toggle";
   public static final String PERM_HOME_ACCESS = "home_access";
   public static final String PERM_HOME_SET = "home_set";
   public static final String PERM_MANAGE_MEMBERS = "manage_members";
   public static final String PERM_DELETE_TEAM = "delete_team";

   public TeamRankManager(Team plugin) {
      this.plugin = plugin;
   }

   public boolean hasPermission(String playerName, String teamName, String permission) {
      DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
      if (team == null) {
         return false;
      } else if (team.isLeader(playerName)) {
         return true;
      } else {
         FileConfiguration config = team.getTeamConfig();
         return config.getBoolean("permissions." + playerName + "." + permission, false);
      }
   }

   public void setPermission(String playerName, String teamName, String permission, boolean value) {
      DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
      if (team != null) {
         team.getTeamConfig().set("permissions." + playerName + "." + permission, value);
         team.saveToFile();
      }
   }

   public void removePlayerPermissions(String playerName, String teamName) {
      DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
      if (team != null) {
         team.getTeamConfig().set("permissions." + playerName, (Object)null);
         team.saveToFile();
      }
   }

   public void loadAllPermissions() {
   }

   public void saveAllPermissions() {
   }

   public void removeTeamPermissions(String teamName) {
      DataManager team = this.plugin.getTeamManager().getTeamByName(teamName);
      if (team != null) {
         team.getTeamConfig().set("permissions", (Object)null);
         team.saveToFile();
      }
   }
}
