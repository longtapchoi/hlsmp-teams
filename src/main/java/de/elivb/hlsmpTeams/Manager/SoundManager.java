package de.elivb.hlsmpTeams.Manager;

import de.elivb.hlsmpTeams.Team;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {
   private final Team plugin;

   public SoundManager(Team plugin) {
      this.plugin = plugin;
   }

   public void playSound(Player player, String soundKey) {
      if (player != null) {
         if (this.plugin.isFolia()) {
            this.plugin.runTask(player, () -> this.playSoundInternal(player, soundKey));
         } else {
            this.playSoundInternal(player, soundKey);
         }

      }
   }

   private void playSoundInternal(Player player, String soundKey) {
      try {
         switch (soundKey) {
            case "click-sound":
               player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
               break;
            case "plugin-reloaded":
            case "success-sound":
               player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
               break;
            case "error-sound":
               player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               break;
            case "teleport-cooldown":
               player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
               break;
            case "teleport-complete":
               player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
               break;
            case "teleport-cancel":
               player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               break;
            default:
               player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
         }
      } catch (Exception var6) {
         try {
            String soundName = this.getSoundString(soundKey);
            player.playSound(player.getLocation(), soundName, 1.0F, 1.0F);
         } catch (Exception var5) {
         }
      }

   }

   private String getSoundString(String soundKey) {
      switch (soundKey) {
         case "click-sound":
            return "minecraft:ui.button.click";
         case "plugin-reloaded":
         case "success-sound":
            return "minecraft:entity.player.levelup";
         case "error-sound":
            return "minecraft:entity.villager.no";
         case "teleport-cooldown":
            return "minecraft:block.note_block.pling";
         case "teleport-complete":
            return "minecraft:entity.enderman.teleport";
         case "teleport-cancel":
            return "minecraft:entity.villager.no";
         default:
            return "minecraft:block.note_block.pling";
      }
   }
}
