package de.elivb.hlsmpTeams;

import de.elivb.hlsmpTeams.Manager.DataManager;
import de.elivb.hlsmpTeams.Manager.TeamManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Papi1 extends PlaceholderExpansion {
   private final Team plugin;

   public Papi1(Team plugin) {
      this.plugin = plugin;
   }

   public @NotNull String getIdentifier() {
      return "donutteam";
   }

   public @NotNull String getAuthor() {
      return "elivb";
   }

   public @NotNull String getVersion() {
      return "1.0";
   }

   public boolean persist() {
      return true;
   }

   public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
      if (player == null) {
         return "";
      } else {
         TeamManager teamManager = this.plugin.getTeamManager();
         DataManager team = teamManager.getPlayerTeam(player.getName());
         if (team != null) {
            String cleanName = team.getName().replaceAll("&[0-9a-fk-or]", "").replaceAll("§[0-9a-fk-or]", "");
            return cleanName;
         } else {
            return "N/A";
         }
      }
   }
}
