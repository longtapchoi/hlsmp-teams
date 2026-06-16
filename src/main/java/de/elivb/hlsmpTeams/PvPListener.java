package de.elivb.hlsmpTeams;

import de.elivb.hlsmpTeams.Manager.DataManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;

public class PvPListener implements Listener {
   private final Team plugin;

   public PvPListener(Team plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onPlayerDamage(EntityDamageByEntityEvent event) {
      Player attacker = null;
      if (event.getDamager() instanceof Player) {
         attacker = (Player)event.getDamager();
      } else if (event.getDamager() instanceof Projectile) {
         Projectile proj = (Projectile)event.getDamager();
         ProjectileSource shooter = proj.getShooter();
         if (shooter instanceof Player) {
            attacker = (Player)shooter;
         }
      }

      if (attacker != null) {
         if (event.getEntity() instanceof Player) {
            Player victim = (Player)event.getEntity();
            if (!attacker.getUniqueId().equals(victim.getUniqueId())) {
               DataManager attackerTeam = this.plugin.getTeamManager().getPlayerTeam(attacker.getName());
               DataManager victimTeam = this.plugin.getTeamManager().getPlayerTeam(victim.getName());
               if (attackerTeam != null && victimTeam != null && attackerTeam.getName().equalsIgnoreCase(victimTeam.getName())) {
                  boolean pvpEnabled = attackerTeam.getTeamConfig().getBoolean("pvp-enabled", false);
                  if (!pvpEnabled) {
                     event.setCancelled(true);
                     String msg = this.plugin.getLangManager().getMessage("team.pvp-disabled-damage");
                     if (msg != null && !msg.isEmpty()) {
                        attacker.sendMessage(msg);
                     }
                  }
               }

            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onFallDamage(EntityDamageEvent event) {
      if (event.getCause() != DamageCause.FALL) {
         ;
      }
   }
}
