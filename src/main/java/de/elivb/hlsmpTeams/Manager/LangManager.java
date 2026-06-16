package de.elivb.hlsmpTeams.Manager;

import de.elivb.hlsmpTeams.HexUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LangManager {
   private JavaPlugin plugin;
   private FileConfiguration langConfig;
   private File langFile;

   public LangManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.setupLangFile();
   }

   private void setupLangFile() {
      if (!this.plugin.getDataFolder().exists()) {
         this.plugin.getDataFolder().mkdir();
      }

      this.langFile = new File(this.plugin.getDataFolder(), "lang.yml");
      if (!this.langFile.exists()) {
         this.plugin.saveResource("lang.yml", false);
      }

      this.langConfig = YamlConfiguration.loadConfiguration(this.langFile);
   }

   public void loadMessages() {
      this.langConfig = YamlConfiguration.loadConfiguration(this.langFile);
   }

   public void saveMessages() {
      try {
         this.langConfig.save(this.langFile);
      } catch (IOException var2) {
      }

   }

   public String getMessage(String path) {
      String message = this.langConfig.getString(path);
      if (message == null) {
         this.plugin.getLogger().warning("Message not found: " + path);
      }

      return this.formatMessage(message);
   }

   public String getMessageWithoutPrefix(String path) {
      String message = this.langConfig.getString(path);
      if (message == null) {
         this.plugin.getLogger().warning("Message not found: " + path);
      }

      return HexUtils.colorize(message);
   }

   public List<String> getMessages(String path) {
      List messages = this.langConfig.getStringList(path);
      ArrayList<String> coloredMessages = new ArrayList();

      for(Object message : messages) {
         coloredMessages.add(this.formatMessage((String)message));
      }

      return coloredMessages;
   }

   public List<String> getMessagesWithoutPrefix(String path) {
      List messages = this.langConfig.getStringList(path);
      ArrayList<String> coloredMessages = new ArrayList();

      for(Object message : messages) {
         coloredMessages.add(HexUtils.colorize((String)message));
      }

      return coloredMessages;
   }

   private String formatMessage(String message) {
      Object formatted = HexUtils.colorize(message);
      if (this.isPrefixEnabled()) {
         String var10000 = this.getPrefix();
         formatted = var10000 + " " + (String)formatted;
      }

      return formatted.toString();
   }

   private boolean isPrefixEnabled() {
      return this.langConfig.getBoolean("prefix-enable", true);
   }

   private String getPrefix() {
      return HexUtils.colorize(this.langConfig.getString("prefix", "&#FC0000&lTEAM &7»"));
   }

   public void setMessage(String path, String value) {
      this.langConfig.set(path, value);
      this.saveMessages();
   }

   public void reloadMessages() {
      this.loadMessages();
   }
}
