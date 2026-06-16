package de.elivb.hlsmpTeams.Manager;

import de.elivb.hlsmpTeams.Team;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class DataManager {
   private final String name;
   private String leader;
   private final LinkedHashSet<String> members;
   private final File teamFile;
   private final FileConfiguration teamConfig;
   private final Team plugin;

   public DataManager(String name, String leader, Team plugin) {
      this.name = name;
      this.leader = leader;
      this.members = new LinkedHashSet();
      this.members.add(leader);
      this.plugin = plugin;
      this.teamFile = this.getTeamFile(name);
      this.teamConfig = YamlConfiguration.loadConfiguration(this.teamFile);
      this.saveToFile();
   }

   public DataManager(String teamName, Team plugin) {
      this.plugin = plugin;
      this.teamFile = this.getTeamFile(teamName);
      this.teamConfig = YamlConfiguration.loadConfiguration(this.teamFile);
      this.name = this.teamConfig.getString("name", teamName);
      this.leader = this.teamConfig.getString("leader", "");
      this.members = new LinkedHashSet();
      if (this.teamConfig.contains("members")) {
         List<String> saved = this.teamConfig.getStringList("members");
         this.members.addAll(saved);
      }

      long fallbackTime = this.teamFile.exists() ? this.teamFile.lastModified() : System.currentTimeMillis();
      boolean changed = false;

      for(String m : this.members) {
         if (!this.teamConfig.contains("joinDates." + m)) {
            this.teamConfig.set("joinDates." + m, fallbackTime > 0L ? fallbackTime : System.currentTimeMillis());
            changed = true;
         }
      }

      if (changed) {
         this.saveToFile();
      }

   }

   private File getTeamFile(String teamName) {
      File teamsFolder = new File(this.plugin.getDataFolder(), "Teams");
      if (!teamsFolder.exists() && !teamsFolder.mkdirs()) {
      }

      return new File(teamsFolder, teamName + ".yml");
   }

   public void saveToFile() {
      if (this.teamConfig != null && this.teamFile != null) {
         try {
            this.teamConfig.set("name", this.name);
            this.teamConfig.set("leader", this.leader);
            ArrayList<String> memberList = new ArrayList(this.members);
            this.teamConfig.set("members", memberList);
            this.teamConfig.save(this.teamFile);
         } catch (IOException var2) {
            if (this.plugin != null) {
            }
         }

      } else {
         if (this.plugin != null) {
         }

      }
   }

   public void setHome(Location location) {
      this.teamConfig.set("home.world", location.getWorld().getName());
      this.teamConfig.set("home.x", location.getX());
      this.teamConfig.set("home.y", location.getY());
      this.teamConfig.set("home.z", location.getZ());
      this.teamConfig.set("home.yaw", location.getYaw());
      this.teamConfig.set("home.pitch", location.getPitch());
      this.saveToFile();
   }

   public Location getHome() {
      if (!this.teamConfig.contains("home")) {
         return null;
      } else {
         String worldName = this.teamConfig.getString("home.world");
         double x = this.teamConfig.getDouble("home.x");
         double y = this.teamConfig.getDouble("home.y");
         double z = this.teamConfig.getDouble("home.z");
         float yaw = (float)this.teamConfig.getDouble("home.yaw", (double)0.0F);
         float pitch = (float)this.teamConfig.getDouble("home.pitch", (double)0.0F);
         return worldName != null && Bukkit.getWorld(worldName) != null ? new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch) : null;
      }
   }

   public boolean hasHome() {
      return this.teamConfig.contains("home");
   }

   public void deleteHome() {
      this.teamConfig.set("home", (Object)null);
      this.saveToFile();
   }

   public String getName() {
      return this.name;
   }

   public String getLeader() {
      return this.leader;
   }

   public Set<String> getMembers() {
      return new LinkedHashSet(this.members);
   }

   public void addMember(String playerName) {
      this.members.add(playerName);
      if (this.teamConfig != null) {
         this.teamConfig.set("joinDates." + playerName, System.currentTimeMillis());
      }

      this.saveToFile();
   }

   public void removeMember(String playerName) {
      this.members.remove(playerName);
      if (this.teamConfig != null) {
         this.teamConfig.set("joinDates." + playerName, (Object)null);
      }

      this.saveToFile();
   }

   public boolean isMember(String playerName) {
      return this.members.contains(playerName);
   }

   public boolean isLeader(String playerName) {
      return Objects.equals(this.leader, playerName);
   }

   public void broadcastToMembers(String message) {
      for(String memberName : this.members) {
         Player player = Bukkit.getPlayer(memberName);
         if (player != null && player.isOnline()) {
            player.sendMessage(message);
         }
      }

   }

   public int getMemberCount() {
      return this.members.size();
   }

   public boolean hasMember(String playerName) {
      return this.isMember(playerName);
   }

   public void setLeader(String newLeader) {
      this.leader = newLeader;
      this.saveToFile();
   }

   public void deleteTeamFile() {
      if (this.teamFile.exists() && !this.teamFile.delete()) {
      }

   }

   public FileConfiguration getTeamConfig() {
      return this.teamConfig;
   }

   public long getMemberJoinTime(String playerName) {
      return this.teamConfig == null ? 0L : this.teamConfig.getLong("joinDates." + playerName, 0L);
   }

   public static boolean teamExists(String teamName, Team plugin) {
      File teamFile = new File(plugin.getDataFolder(), "Teams" + File.separator + teamName + ".yml");
      return teamFile.exists();
   }

   public static List<String> getAllTeamNames(Team plugin) {
      ArrayList<String> teamNames = new ArrayList();
      File teamsFolder = new File(plugin.getDataFolder(), "Teams");
      File[] files;
      if (teamsFolder.exists() && teamsFolder.isDirectory() && (files = teamsFolder.listFiles((dir, name) -> name.endsWith(".yml"))) != null) {
         for(File file : files) {
            String fileName = file.getName();
            teamNames.add(fileName.substring(0, fileName.length() - 4));
         }
      }

      return teamNames;
   }
}
