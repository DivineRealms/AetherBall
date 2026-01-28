package io.github.divinerealms.footcube.managers;

import io.github.divinerealms.footcube.configs.Lang;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigManager {

  @Getter
  private final Plugin plugin;
  private final String folderName;

  private final Map<String, FileConfiguration> configs = new HashMap<>();
  private final Map<String, File> files = new HashMap<>();
  private final Set<String> readOnlyConfigs = new HashSet<>();

  public ConfigManager(Plugin plugin, String folderName) {
    this.plugin = plugin;
    this.folderName = folderName;
    this.readOnlyConfigs.add("settings.yml");
  }

  public void createNewFile(String name, String header) {
    File file = new File(plugin.getDataFolder(), folderName.isEmpty()
        ? name
        : folderName + File.separator + name);

    FileConfiguration cfg;

    if (!file.exists()) {
      copyDefaultsFromResource(name);
      cfg = YamlConfiguration.loadConfiguration(file);
      if (!readOnlyConfigs.contains(name)) {
        cfg.options().header(header);
        cfg.options().copyDefaults(true);
        saveConfig(name);
      }
    } else {
      cfg = YamlConfiguration.loadConfiguration(file);
      if (!readOnlyConfigs.contains(name)) {
        cfg.options().header(header);
        cfg.options().copyDefaults(true);
      }
    }

    configs.put(name, cfg);
    files.put(name, file);
  }

  public FileConfiguration getConfig(String name) {
    if (!configs.containsKey(name)) {
      this.reloadConfig(name);
    }

    return configs.get(name);
  }

  public void reloadAllConfigs() {
    configs.keySet().forEach(this::reloadConfig);
    Lang.setFile(getConfig("messages.yml"));
  }

  public void reloadConfig(String name) {
    File file = new File(plugin.getDataFolder(), folderName.isEmpty()
        ? name
        : folderName + File.separator + name);

    if (!file.exists()) {
      copyDefaultsFromResource(name);
    }

    FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
    files.put(name, file);
    configs.put(name, cfg);
  }

  public void saveConfig(String name) {
    if (readOnlyConfigs.contains(name)) {
      Bukkit.getLogger().info("Skipping save for read-only config: " + name);
      return;
    }

    FileConfiguration cfg = configs.get(name);
    File file = files.get(name);

    if (cfg != null && file != null) {
      try {
        cfg.save(file);
      } catch (IOException exception) {
        plugin.getLogger().info("Could not save config to " + file);
      }
    }
  }

  public void saveAll() {
    configs.keySet().forEach(this::saveConfig);
  }

  private void copyDefaultsFromResource(String name) {
    try {
      String resourcePath = folderName.isEmpty()
          ? name
          : folderName + File.separator + name;
      if (plugin.getResource(resourcePath) != null) {
        plugin.saveResource(resourcePath, false);
        plugin.getLogger().info("Copied default config from JAR: " + resourcePath);
      }
    } catch (Exception ignored) {
    }
  }
}