package de.elivb.hlsmpTeams;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class TeamChatListener implements Listener {
   private final Team plugin;

   public TeamChatListener(Team plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      if (this.plugin.isTeamChatEnabled(player)) {
         event.setCancelled(true);
         String message = event.getMessage();
         this.plugin.sendTeamMessage(player, message);
      }

   }
}
