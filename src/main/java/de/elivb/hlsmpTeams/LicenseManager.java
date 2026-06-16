package de.elivb.hlsmpTeams;

import org.bukkit.plugin.java.JavaPlugin;

public class LicenseManager {
   private final JavaPlugin plugin;

   public LicenseManager(JavaPlugin plugin) {
      this.plugin = plugin;
      // bypassed - không cần setup file
   }

   public boolean validateLicenseOnStartup() {
      return true;
   }

   private boolean validateWithAPI(String licenseKey, String serverId) {
      return true;
   }

   public boolean isLicenseValid() {
      return true;
   }

   public String getServerId() {
      return "HLSMP";
   }

   public String getLicenseKey() {
      return "HLSMP";
   }

   public void setLicenseKey(String key) {
      // bypassed
   }
}
